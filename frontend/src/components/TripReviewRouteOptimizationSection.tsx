import { useState } from "react";
import { ArrowRight, Loader2 } from "lucide-react";
import type { RouteOptimization } from "@/api/reviews";
import { useApplyRouteOptimization } from "@/hooks/useApplyRouteOptimization";
import { readApiErrorMessage } from "@/api/http";
import { Button } from "@/components/ui/button";

const duration = (seconds: number | null | undefined) =>
  typeof seconds === "number" && Number.isFinite(seconds) && seconds >= 0 ? `${Math.round(seconds / 60)}분` : "정보 없음";

export function TripReviewRouteOptimizationSection({ tripId, open, optimization }: {
  tripId: number; open: boolean; optimization: RouteOptimization | null | undefined;
}) {
  const [applied, setApplied] = useState(false);
  const apply = useApplyRouteOptimization(tripId, open, optimization);
  const { itinerary, guard } = apply;
  const days = Array.isArray(optimization?.days) ? optimization.days.filter((day) =>
    day && Array.isArray(day.currentOrder) && Array.isArray(day.optimizedOrder),
  ) : [];
  return (
    <section className="space-y-4 border-t pt-6" aria-label="동선 최적화">
      <h3 className="text-lg font-semibold">동선 최적화</h3>
      {days.map((day, index) => (
        <div key={`${day.visitDay}-${index}`} className="space-y-3 border-b pb-4">
          <h4 className="font-medium">Day {day.visitDay}</h4>
          <div className="grid gap-4 sm:grid-cols-2">
            {[{ title: "현재 순서", items: day.currentOrder, seconds: day.currentDurationSeconds },
              { title: "최적화 순서", items: day.optimizedOrder, seconds: day.optimizedDurationSeconds }].map((order) => (
              <div key={order.title} className="min-w-0">
                <p className="text-sm font-medium">{order.title} · {duration(order.seconds)}</p>
                <ol className="mt-2 list-decimal space-y-1 pl-5 text-sm break-words">
                  {order.items.map((item, i) => <li key={i}>{typeof item?.name === "string" ? item.name : "일정 정보 없음"}</li>)}
                </ol>
              </div>
            ))}
          </div>
          <p className="text-sm">예상 절약 시간: {duration(day.savedDurationSeconds)}</p>
          {Array.isArray(day.warnings) && day.warnings.filter((warning) => typeof warning === "string").map((warning, i) => (
            <p key={i} className="text-sm text-amber-700 dark:text-amber-400 break-words">{warning}</p>
          ))}
        </div>
      ))}
      <p className="text-sm text-muted-foreground">시간과 메모는 유지됩니다. 동시 편집이 있으면 적용 직전 확인 이후에도 일정이 달라질 수 있습니다.</p>
      {applied ? <p role="status" className="text-sm">최적화 순서를 적용했습니다.</p> : <>
        {itinerary.isError ? <p role="alert" className="text-sm text-destructive">최신 일정을 불러오지 못했습니다.</p> :
          guard.ok === false && <p role="status" className="text-sm text-muted-foreground">{guard.message}</p>}
        {apply.error && <p role="alert" className="text-sm text-destructive">{readApiErrorMessage(apply.error, apply.error.message || "순서 적용에 실패했습니다. 최신 일정을 확인해주세요.")}</p>}
        <Button disabled={!open || !guard.ok || itinerary.isError || itinerary.isFetching || apply.isPending}
          onClick={async () => { if (optimization && await apply.apply(optimization)) setApplied(true); }}>
          {apply.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <ArrowRight className="mr-2 h-4 w-4" />}
          {apply.isPending ? "최신 일정 확인 및 적용 중..." : "최적화 순서 적용"}
        </Button>
      </>}
    </section>
  );
}
