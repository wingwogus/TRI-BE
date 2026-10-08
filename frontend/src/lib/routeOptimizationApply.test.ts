import test from "node:test";
import assert from "node:assert/strict";
import type { RouteOptimization, RouteOptimizationItem } from "../api/reviews.ts";
import { buildRouteOptimizationApply, applyFreshRouteOptimization } from "./routeOptimizationApply.ts";

const item = (itemId: number, itemOrder: number): RouteOptimizationItem => ({
  itemId, itemOrder, name: `Place ${itemId}`, placeId: null, externalPlaceId: null,
});
const day = (visitDay = 1, ids = [1, 2]) => ({
  visitDay,
  currentOrder: ids.map((id, index) => item(id, index + 1)),
  optimizedOrder: [...ids].reverse().map((id, index) => item(id, index + 1)),
  currentDurationSeconds: 120, optimizedDurationSeconds: 60, savedDurationSeconds: 60,
  currentLegs: [], optimizedLegs: [], warnings: [],
});
const route = (): RouteOptimization => ({ days: [day(), day(2, [3, 4])] });
const live = () => [
  { itineraryId: 1, visitDay: 1, itemOrder: 1, time: "09:30", memo: "Keep me" },
  { itineraryId: 2, visitDay: 1, itemOrder: 2, time: null, memo: null },
  { itineraryId: 3, visitDay: 2, itemOrder: 1, time: "12:00", memo: "Lunch" },
  { itineraryId: 4, visitDay: 2, itemOrder: 2, time: null, memo: null },
];

test("flattens full affected days into one order-only payload, preserving input time/memo", () => {
  const itinerary = live();
  const before = structuredClone(itinerary);
  const result = buildRouteOptimizationApply(route(), itinerary);
  assert.deepEqual(result, { ok: true, request: { items: [
    { itemId: 2, visitDay: 1, itemOrder: 1 }, { itemId: 1, visitDay: 1, itemOrder: 2 },
    { itemId: 4, visitDay: 2, itemOrder: 1 }, { itemId: 3, visitDay: 2, itemOrder: 2 },
  ] } });
  assert.deepEqual(itinerary, before);
});

test("sorts live items by order and ignores unaffected days", () => {
  assert.equal(buildRouteOptimizationApply(route(), [
    ...live().reverse(), { itineraryId: 5, visitDay: 3, itemOrder: 1 },
  ]).ok, true);
});

test("blocks added, removed, moved and reordered affected-day items", () => {
  const variants = [
    [...live(), { itineraryId: 5, visitDay: 1, itemOrder: 3 }],
    live().slice(1),
    live().map((entry) => entry.itineraryId === 1 ? { ...entry, visitDay: 3 } : entry),
    live().map((entry) => entry.visitDay === 1 ? { ...entry, itemOrder: 3 - entry.itemOrder } : entry),
    live().map((entry) => ({ ...entry, itemOrder: entry.itemOrder + 1 })),
  ];
  for (const itinerary of variants) {
    assert.equal(buildRouteOptimizationApply(route(), itinerary).ok, false);
  }
});

test("blocks duplicate IDs, duplicate days, invalid orders and cross-day membership changes", () => {
  const variants = [route(), route(), route(), route(), route(), route()];
  variants[0].days[0].optimizedOrder[1] = item(2, 2);
  variants[1].days.push(day());
  variants[2].days[0].optimizedOrder[0].itemOrder = 2;
  variants[3].days[0].currentOrder.reverse();
  variants[4].days[0].optimizedOrder[0] = item(3, 1);
  variants[5].days[0].currentOrder[0].itemId = NaN;
  for (const optimization of variants) {
    assert.equal(buildRouteOptimizationApply(optimization, live()).ok, false);
  }
  assert.equal(buildRouteOptimizationApply(route(), [...live(), live()[0]]).ok, false);
});

test("fails closed on missing or malformed route and live data", () => {
  for (const optimization of [null, undefined, {}, { days: [] }, { days: [null] }, { days: [{}] }]) {
    assert.equal(buildRouteOptimizationApply(optimization as RouteOptimization, live()).ok, false);
  }
  assert.equal(buildRouteOptimizationApply(route(), undefined).ok, false);
  assert.equal(buildRouteOptimizationApply(route(), [null] as never).ok, false);
});

test("blocks no-op and includes unchanged days when another day changes", () => {
  const optimization = route();
  optimization.days[0].optimizedOrder = optimization.days[0].currentOrder;
  const mixed = buildRouteOptimizationApply(optimization, live());
  assert.equal(mixed.ok, true);
  if (mixed.ok) assert.equal(mixed.request.items.length, 4);
  optimization.days[1].optimizedOrder = optimization.days[1].currentOrder;
  const result = buildRouteOptimizationApply(optimization, live());
  assert.equal(result.ok, false);
  if (result.ok === false) assert.equal(result.reason, "no-op");
});

test("allows gaps in current order but requires complete contiguous optimized order", () => {
  const optimization = route();
  optimization.days[0].currentOrder[1].itemOrder = 4;
  const itinerary = live();
  itinerary[1].itemOrder = 4;
  assert.equal(buildRouteOptimizationApply(optimization, itinerary).ok, true);
  optimization.days[0].optimizedOrder[1].itemOrder = 4;
  assert.equal(buildRouteOptimizationApply(optimization, itinerary).ok, false);
});

test("fresh GET immediately precedes exactly one bulk PATCH", async () => {
  const calls: string[] = [];
  await applyFreshRouteOptimization(route(), {
    fetchItinerary: async () => { calls.push("GET"); return live(); },
    reorder: async (request) => { calls.push(`PATCH:${request.items.length}`); },
  });
  assert.deepEqual(calls, ["GET", "PATCH:4"]);
});

test("stale fresh GET and failed GET send no PATCH", async () => {
  let patches = 0;
  for (const fetchItinerary of [async () => live().slice(1), async () => { throw new Error("offline"); }]) {
    await assert.rejects(() => applyFreshRouteOptimization(route(), {
      fetchItinerary, reorder: async () => { patches += 1; },
    }));
  }
  assert.equal(patches, 0);
});

test("failed PATCH is not retried", async () => {
  let patches = 0;
  await assert.rejects(() => applyFreshRouteOptimization(route(), {
    fetchItinerary: async () => live(),
    reorder: async () => { patches += 1; throw new Error("network"); },
  }));
  assert.equal(patches, 1);
});
