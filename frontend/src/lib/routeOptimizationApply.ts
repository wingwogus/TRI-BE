import type { OrderUpdateRequest } from "../api/itinerary.ts";
import type { RouteOptimization, RouteOptimizationItem } from "../api/reviews.ts";

type LiveItem = { itineraryId: number; visitDay: number; itemOrder: number };
type BlockReason = "missing" | "invalid" | "stale" | "no-op";
export type RouteOptimizationApplyResult =
  | { ok: true; request: OrderUpdateRequest }
  | { ok: false; reason: BlockReason; message: string };

const messages: Record<BlockReason, string> = {
  missing: "동선 또는 최신 일정 정보가 없어 적용할 수 없습니다. 새 분석을 받아주세요.",
  invalid: "동선의 일정 구성이나 순서 정보가 올바르지 않습니다. 새 분석을 받아주세요.",
  stale: "분석 이후 일정이 변경되었습니다. 최신 일정으로 새 분석을 받아주세요.",
  "no-op": "현재와 동일한 순서입니다. 적용할 변경 사항이 없습니다.",
};
const block = (reason: BlockReason): RouteOptimizationApplyResult => ({ ok: false, reason, message: messages[reason] });
const positiveInteger = (value: number) => Number.isSafeInteger(value) && value > 0;
const validSequence = (items: RouteOptimizationItem[]) =>
  Array.isArray(items) && items.length > 0 && items.every((item, index) =>
    item && positiveInteger(item.itemId) && positiveInteger(item.itemOrder) &&
    (index === 0 || item.itemOrder > items[index - 1].itemOrder),
  ) && new Set(items.map((item) => item.itemId)).size === items.length;

export function buildRouteOptimizationApply(
  optimization: RouteOptimization | null | undefined,
  itinerary: readonly LiveItem[] | null | undefined,
): RouteOptimizationApplyResult {
  if (!Array.isArray(optimization?.days) || !optimization.days.length || !Array.isArray(itinerary)) return block("missing");
  const days = new Set<number>();
  const ids = new Set<number>();
  const items: OrderUpdateRequest["items"] = [];
  let changed = false;

  if (itinerary.some((item) => !item || !positiveInteger(item.itineraryId) ||
    !positiveInteger(item.visitDay) || !positiveInteger(item.itemOrder)) ||
    new Set(itinerary.map((item) => item.itineraryId)).size !== itinerary.length) return block("invalid");

  for (const day of optimization.days) {
    if (!day || !positiveInteger(day.visitDay) || days.has(day.visitDay) ||
      !validSequence(day.currentOrder) || !validSequence(day.optimizedOrder)) return block("invalid");
    days.add(day.visitDay);
    const currentIds = new Set(day.currentOrder.map((item) => item.itemId));
    if (day.optimizedOrder.length !== day.currentOrder.length ||
      day.optimizedOrder.some((item) => !currentIds.has(item.itemId))) return block("invalid");
    const dayChanged = day.optimizedOrder.some((item, index) => item.itemId !== day.currentOrder[index].itemId);
    // Unchanged backend days may retain gaps; changed sequences must be one-based and complete.
    if (day.optimizedOrder.some((item, index) => item.itemOrder !==
      (dayChanged ? index + 1 : day.currentOrder[index].itemOrder))) return block("invalid");
    for (const id of currentIds) {
      if (ids.has(id)) return block("invalid");
      ids.add(id);
    }
    const liveDay = itinerary.filter((item) => item.visitDay === day.visitDay)
      .sort((a, b) => a.itemOrder - b.itemOrder);
    if (liveDay.length !== day.currentOrder.length || liveDay.some((item, index) =>
      item.itineraryId !== day.currentOrder[index].itemId || item.itemOrder !== day.currentOrder[index].itemOrder,
    )) return block("stale");
    changed ||= dayChanged;
    items.push(...day.optimizedOrder.map((item) => ({
      itemId: item.itemId, visitDay: day.visitDay, itemOrder: item.itemOrder,
    })));
  }
  return changed ? { ok: true, request: { items } } : block("no-op");
}

export async function applyFreshRouteOptimization<T>(
  optimization: RouteOptimization | null | undefined,
  actions: { fetchItinerary: () => Promise<readonly LiveItem[]>; reorder: (request: OrderUpdateRequest) => Promise<T> },
): Promise<T> {
  const live = await actions.fetchItinerary();
  const result = buildRouteOptimizationApply(optimization, live);
  if (result.ok === false) throw new Error(result.message);
  // A fresh read reduces staleness but cannot ensure atomicity without a server version precondition.
  return actions.reorder(result.request);
}
