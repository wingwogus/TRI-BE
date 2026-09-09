import { memo } from "react";
import { AlertTriangle, Clock } from "lucide-react";
import type { AiTripProposal } from "@/lib/aiTripGeneration";

export const AiTripProposalPreview = memo(function AiTripProposalPreview({ proposal }: { proposal: AiTripProposal }) {
  return (
    <div className="min-w-0 space-y-5 break-words">
      <div>
        <h3 className="text-lg font-semibold">{proposal.title}</h3>
        <p className="mt-1 text-sm text-muted-foreground">{proposal.startDate} ~ {proposal.endDate}</p>
        {proposal.summary && <p className="mt-3 text-sm leading-relaxed">{proposal.summary}</p>}
      </div>
      {proposal.warnings.length > 0 && (
        <div className="border-l-2 border-amber-500 bg-amber-50 p-3 text-sm text-amber-950">
          <p className="mb-2 flex items-center gap-2 font-medium"><AlertTriangle className="h-4 w-4 shrink-0" />확인할 사항</p>
          <ul className="list-disc space-y-1 pl-5">{proposal.warnings.map((warning, index) => <li key={index}>{warning}</li>)}</ul>
        </div>
      )}
      {proposal.days.map((day) => (
        <section key={day.visitDay} className="border-t pt-4" aria-label={`${day.visitDay}일차 일정`}>
          <h4 className="mb-3 font-semibold">{day.visitDay}일차{day.theme && ` · ${day.theme}`}</h4>
          <ol className="divide-y">
            {day.items.map((item) => (
              <li key={item.order} className="grid grid-cols-[3rem_minmax(0,1fr)] gap-3 py-3 text-sm">
                <time className="font-medium tabular-nums text-muted-foreground">{item.time}</time>
                <div className="min-w-0">
                  <p className="font-medium">{item.placeName}</p>
                  {item.memo && <p className="mt-1 leading-relaxed text-muted-foreground">{item.memo}</p>}
                  {item.durationMinutes != null && item.durationMinutes > 0 && <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground"><Clock className="h-3 w-3" />{item.durationMinutes}분</p>}
                </div>
              </li>
            ))}
          </ol>
        </section>
      ))}
    </div>
  );
});
