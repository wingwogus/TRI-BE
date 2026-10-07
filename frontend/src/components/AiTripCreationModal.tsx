import { useState, type FormEvent } from "react";
import { ZodError } from "zod";
import { ArrowRight, Loader2, Sparkles } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AiTripProposalPreview } from "@/components/AiTripProposalPreview";
import { useAiTripGeneration } from "@/hooks/useAiTripGeneration";
import { useToast } from "@/hooks/use-toast";
import { readApiErrorMessage } from "@/api/http";
import { TRIP_COUNTRY_OPTIONS, getTripRegionsByCountryCode } from "@/lib/tripRegions";
import { aiTripCreateSchema, companionOptions, travelStyleOptions, type TripTravelStyle } from "@/lib/aiTripGeneration";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onTripCreated: (tripId: number) => void;
}

export default function AiTripCreationModal({ open, onOpenChange, onTripCreated }: Props) {
  const { toast } = useToast();
  const ai = useAiTripGeneration(open);
  const [countryCode, setCountryCode] = useState("");
  const [regionCode, setRegionCode] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [title, setTitle] = useState("");
  const [companionType, setCompanionType] = useState("");
  const [travelStyles, setTravelStyles] = useState<TripTravelStyle[]>([]);
  const [allowTextOnlyItems, setAllowTextOnlyItems] = useState(true);
  const [validationError, setValidationError] = useState("");
  const regions = getTripRegionsByCountryCode(countryCode);
  const busy = ai.isGenerating || ai.isApplying;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const parsed = aiTripCreateSchema.safeParse({ regionCode, startDate, endDate, title: title.trim() || undefined, companionType, travelStyles });
    if (!companionType) { setValidationError("동행자 유형을 선택해주세요."); return; }
    if (!parsed.success) { setValidationError(parsed.error.issues[0].message); return; }
    setValidationError("");
    try { await ai.generate(parsed.data); } catch { /* The hook retains the error and input for retry. */ }
  };

  const apply = async () => {
    try {
      const result = await ai.apply(allowTextOnlyItems);
      if (!result) return;
      toast({
        title: "AI 여행 생성 완료",
        description: result.unresolvedPlaceCount > 0
          ? `${result.createdItemCount}개 일정 중 ${result.unresolvedPlaceCount}개는 지도 장소 없이 저장됐습니다.`
          : `${result.createdItemCount}개 일정이 저장됐습니다.`,
      });
      onTripCreated(result.tripId);
    } catch { /* A retry applies the same persisted proposal. */ }
  };

  const requestError = ai.error instanceof ZodError
    ? "응답 내용을 확인할 수 없습니다. 다시 시도해주세요."
    : ai.error ? readApiErrorMessage(ai.error, "요청을 완료하지 못했습니다. 잠시 후 다시 시도해주세요.") : "";

  return (
    <Dialog open={open} onOpenChange={(next) => { if (!ai.isApplying) onOpenChange(next); }}>
      <DialogContent className="max-h-[90dvh] w-[calc(100%-2rem)] max-w-2xl grid-rows-[auto_minmax(0,1fr)] overflow-hidden rounded-lg p-4 sm:p-6">
        <DialogHeader className="pr-6 text-left">
          <DialogTitle className="flex items-center gap-2 tracking-normal"><Sparkles className="h-5 w-5 text-primary" />AI 여행 만들기</DialogTitle>
          <DialogDescription>최대 4박 5일 · 하루 5~6곳의 아침부터 저녁까지 이어지는 동선 · 여행 스타일 1~3개</DialogDescription>
        </DialogHeader>
        <div className="min-h-0 min-w-0 overflow-y-auto overscroll-contain pr-1" aria-busy={busy}>
          {(validationError || requestError) && <p role="alert" className="mb-4 break-words rounded-md border border-destructive/30 p-3 text-sm text-destructive">{validationError || requestError}</p>}
          {ai.isGenerating ? (
            <div role="status" className="flex min-h-48 flex-col items-center justify-center gap-3 py-10">
              <Loader2 className="h-7 w-7 animate-spin text-primary" />
              <p className="text-sm">여행 일정을 작성하고 있습니다...</p>
            </div>
          ) : ai.proposalId ? (
            ai.proposal ? (
              <>
                <AiTripProposalPreview proposal={ai.proposal} />
                {ai.proposal.status === "READY" && <div className="mt-4 flex items-start gap-2 border-t pt-4 text-sm">
                  <Checkbox id="ai-text-only" checked={allowTextOnlyItems} disabled={ai.isApplying} onCheckedChange={(value) => setAllowTextOnlyItems(value === true)} />
                  <Label htmlFor="ai-text-only" className="leading-relaxed">지도에서 찾지 못한 장소도 텍스트 일정으로 저장</Label>
                </div>}
                {ai.proposal.status === "FAILED" && <p role="alert" className="mt-4 text-sm text-destructive">사용할 수 없는 초안입니다. 새 일정을 생성해주세요.</p>}
                <div className="sticky bottom-0 mt-4 flex flex-wrap justify-end gap-2 border-t bg-background py-3">
                  <Button variant="outline" disabled={busy} onClick={() => { ai.startOver(); setValidationError(""); }}>다시 계획하기</Button>
                  {ai.proposal.status === "APPLIED" ? (
                    <Button onClick={() => onTripCreated(ai.proposal!.createdTripId!)}>생성된 여행 열기<ArrowRight className="ml-2 h-4 w-4" /></Button>
                  ) : (
                    <Button disabled={busy || ai.isFetching || ai.proposal.status !== "READY"} onClick={apply}>
                      {ai.isApplying && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}{ai.isApplying ? "여행에 적용 중..." : "내 여행에 적용"}
                    </Button>
                  )}
                </div>
              </>
            ) : (
              <div className="flex min-h-40 items-center justify-center">
                {ai.isFetching ? <p role="status">초안을 불러오는 중...</p> : <div className="flex gap-2"><Button variant="outline" onClick={ai.startOver}>새 일정 작성</Button><Button onClick={() => void ai.reload()}>다시 불러오기</Button></div>}
              </div>
            )
          ) : (
            <form onSubmit={submit} className="space-y-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-2"><Label htmlFor="ai-country">국가</Label>
                  <Select value={countryCode} onValueChange={(value) => { setCountryCode(value); setRegionCode(""); }}>
                    <SelectTrigger id="ai-country"><SelectValue placeholder="국가 선택" /></SelectTrigger>
                    <SelectContent>{TRIP_COUNTRY_OPTIONS.map((country) => <SelectItem key={country.code2} value={country.code2}>{country.name}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="space-y-2"><Label htmlFor="ai-region">여행 권역</Label>
                  <Select value={regionCode} onValueChange={setRegionCode} disabled={!countryCode}>
                    <SelectTrigger id="ai-region"><SelectValue placeholder="권역 선택" /></SelectTrigger>
                    <SelectContent>{regions.map((region) => <SelectItem key={region.code} value={region.code}>{region.label}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="min-w-0 space-y-2"><Label htmlFor="ai-start">시작일</Label><Input className="min-w-0" id="ai-start" type="date" required value={startDate} onChange={(event) => setStartDate(event.target.value)} /></div>
                <div className="min-w-0 space-y-2"><Label htmlFor="ai-end">종료일</Label><Input className="min-w-0" id="ai-end" type="date" required min={startDate || undefined} value={endDate} onChange={(event) => setEndDate(event.target.value)} /></div>
              </div>
              <div className="space-y-2"><Label htmlFor="ai-companion">동행</Label>
                <Select value={companionType} onValueChange={setCompanionType}>
                  <SelectTrigger id="ai-companion"><SelectValue placeholder="동행 선택" /></SelectTrigger>
                  <SelectContent>{companionOptions.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}</SelectContent>
                </Select>
              </div>
              <fieldset className="space-y-3"><legend className="text-sm font-medium">여행 스타일 <span className="text-muted-foreground">({travelStyles.length}/3)</span></legend>
                <div className="grid grid-cols-2 gap-x-3 gap-y-3 sm:grid-cols-3">
                  {travelStyleOptions.map((option) => <div key={option.value} className="flex items-center gap-2">
                    <Checkbox id={`ai-style-${option.value}`} checked={travelStyles.includes(option.value)} disabled={travelStyles.length === 3 && !travelStyles.includes(option.value)}
                      onCheckedChange={(checked) => setTravelStyles((previous) => checked === true ? [...previous, option.value] : previous.filter((value) => value !== option.value))} />
                    <Label htmlFor={`ai-style-${option.value}`}>{option.label}</Label>
                  </div>)}
                </div>
              </fieldset>
              <div className="space-y-2"><Label htmlFor="ai-title">여행 이름 (선택)</Label><Input id="ai-title" maxLength={80} value={title} onChange={(event) => setTitle(event.target.value)} /></div>
              <div className="flex justify-end gap-2 border-t pt-4">
                <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>닫기</Button>
                <Button type="submit" disabled={busy}><Sparkles className="mr-2 h-4 w-4" />일정 생성</Button>
              </div>
            </form>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
