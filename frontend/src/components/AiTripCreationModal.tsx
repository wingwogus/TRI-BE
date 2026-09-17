import { useState, type FormEvent } from "react";
import { ArrowLeft, ArrowRight, Loader2, Sparkles } from "lucide-react";
import { ZodError } from "zod";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Progress } from "@/components/ui/progress";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AiTripProposalPreview } from "@/components/AiTripProposalPreview";
import { useAiTripGeneration } from "@/hooks/useAiTripGeneration";
import { useToast } from "@/hooks/use-toast";
import { readApiErrorMessage } from "@/api/http";
import { TRIP_COUNTRY_OPTIONS, getTripRegionsByCountryCode } from "@/lib/tripRegions";
import { aiTripCreateSchema, companionOptions, travelStyleOptions, type TripTravelStyle } from "@/lib/aiTripGeneration";

interface Props { open: boolean; onOpenChange: (open: boolean) => void; onTripCreated: (tripId: number) => void; }
const TOTAL_STEPS = 6;
const getDateMessage = (start: string, end: string) => {
  if (!start || !end) return "";
  const nights = (Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86_400_000;
  if (nights < 0) return "종료일은 시작일보다 이전일 수 없습니다.";
  return nights > 4 ? "AI 일정 생성은 최대 4박 5일까지 가능합니다." : "";
};

export default function AiTripCreationModal({ open, onOpenChange, onTripCreated }: Props) {
  const { toast } = useToast();
  const ai = useAiTripGeneration(open);
  const [step, setStep] = useState(1);
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
  const dateMessage = getDateMessage(startDate, endDate);
  const busy = ai.isGenerating || ai.isApplying;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const parsed = aiTripCreateSchema.safeParse({ regionCode, startDate, endDate, title: title.trim() || undefined, companionType, travelStyles });
    if (!parsed.success) { setValidationError(parsed.error.issues[0].message); return; }
    setValidationError("");
    try { await ai.generate(parsed.data); } catch { /* hook owns the error state */ }
  };

  const apply = async () => {
    try {
      const result = await ai.apply(allowTextOnlyItems);
      if (!result) return;
      toast({ title: "AI 여행 생성 완료", description: result.unresolvedPlaceCount > 0 ? `${result.createdItemCount}개 일정 중 ${result.unresolvedPlaceCount}개는 지도 장소 없이 저장됐습니다.` : `${result.createdItemCount}개 일정이 저장됐습니다.` });
      onTripCreated(result.tripId);
    } catch { /* hook owns the error state */ }
  };

  const next = () => {
    setValidationError("");
    if (step === 1 && !countryCode) return setValidationError("국가를 선택해주세요.");
    if (step === 2 && !regionCode) return setValidationError("여행 권역을 선택해주세요.");
    if (step === 3 && (!startDate || !endDate)) return setValidationError("여행 시작일과 종료일을 선택해주세요.");
    if (step === 3 && dateMessage) return setValidationError(dateMessage);
    if (step === 4 && !companionType) return setValidationError("동행자 유형을 선택해주세요.");
    if (step === 5 && travelStyles.length === 0) return setValidationError("여행 스타일을 하나 이상 선택해주세요.");
    setStep((current) => Math.min(TOTAL_STEPS, current + 1));
  };

  const startOver = () => {
    if (busy) return;
    ai.startOver(); setStep(1); setCountryCode(""); setRegionCode(""); setStartDate(""); setEndDate(""); setTitle(""); setCompanionType(""); setTravelStyles([]); setValidationError("");
  };
  const stepTitle = ["국가를 선택해주세요", "어디로 떠날까요?", "언제 떠날까요?", "누구와 함께하나요?", "어떤 여행을 원하나요?", "여행 이름을 정해주세요"][step - 1];
  const requestError = ai.error instanceof ZodError ? "응답 내용을 확인할 수 없습니다. 다시 시도해주세요." : ai.error ? readApiErrorMessage(ai.error, "요청을 완료하지 못했습니다. 잠시 후 다시 시도해주세요.") : "";

  return <Dialog open={open} onOpenChange={(nextOpen) => { if (!ai.isApplying) onOpenChange(nextOpen); }}>
    <DialogContent className="min-h-[34rem] max-h-[90dvh] w-[calc(100%-0.75rem)] max-w-3xl grid-rows-[auto_minmax(0,1fr)] overflow-hidden rounded-lg p-5 sm:min-h-[38rem] sm:p-8">
      <DialogHeader className="pr-7 text-left"><DialogTitle className="flex items-center gap-2 tracking-normal"><Sparkles className="h-5 w-5 shrink-0 text-primary" />AI 여행 만들기</DialogTitle><DialogDescription>여행 정보를 하나씩 선택하면 조건에 맞는 일정을 만들어드려요.</DialogDescription></DialogHeader>
      <div className="min-h-0 min-w-0 overflow-y-auto overscroll-contain px-0" aria-busy={busy}>
        {(validationError || requestError) && <p role="alert" className="mb-4 break-words rounded-md border border-destructive/30 p-3 text-sm text-destructive">{validationError || requestError}</p>}
        {ai.isGenerating ? <div role="status" className="flex min-h-48 flex-col items-center justify-center gap-3 py-10"><Loader2 className="h-7 w-7 animate-spin text-primary" /><p className="text-sm">여행 일정을 작성하고 있습니다...</p></div> : ai.proposalId ? (
          ai.proposal ? <><AiTripProposalPreview proposal={ai.proposal} />{ai.proposal.status === "READY" && <div className="mt-4 flex items-start gap-2 border-t pt-4 text-sm"><Checkbox id="ai-text-only" checked={allowTextOnlyItems} disabled={ai.isApplying} onCheckedChange={(value) => setAllowTextOnlyItems(value === true)} /><Label htmlFor="ai-text-only" className="leading-relaxed">지도에서 찾지 못한 장소도 텍스트 일정으로 저장</Label></div>}{ai.proposal.status === "FAILED" && <p role="alert" className="mt-4 text-sm text-destructive">사용할 수 없는 초안입니다. 새 일정을 생성해주세요.</p>}<div className="sticky bottom-0 mt-4 flex flex-wrap justify-end gap-2 border-t bg-background py-3"><Button variant="outline" disabled={busy} onClick={startOver}>다시 계획하기</Button>{ai.proposal.status === "APPLIED" ? <Button onClick={() => onTripCreated(ai.proposal!.createdTripId!)}>생성된 여행 열기<ArrowRight className="ml-2 h-4 w-4" /></Button> : <Button disabled={busy || ai.isFetching || ai.proposal.status !== "READY"} onClick={apply}>{ai.isApplying && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}{ai.isApplying ? "여행에 적용 중..." : "내 여행에 적용"}</Button>}</div></> : <div className="flex min-h-40 items-center justify-center">{ai.isFetching ? <p role="status">초안을 불러오는 중...</p> : <div className="flex gap-2"><Button variant="outline" onClick={startOver}>새 일정 작성</Button><Button onClick={() => void ai.reload()}>다시 불러오기</Button></div>}</div>
        ) : <form onSubmit={submit} className="space-y-5 pt-2"><div className="flex items-center gap-3"><Progress value={(step / TOTAL_STEPS) * 100} className="h-2" /><span className="shrink-0 text-xs tabular-nums text-muted-foreground">{step}/{TOTAL_STEPS}</span></div><div className="min-h-[250px] space-y-5"><div><p className="text-lg font-semibold">{stepTitle}</p><p className="mt-1 text-sm text-muted-foreground">{step === 3 ? "최대 4박 5일까지 선택할 수 있어요." : step === 5 ? "최대 3개까지 골라주세요." : ""}</p></div>
          {step === 1 && <Select value={countryCode} onValueChange={(value) => { setCountryCode(value); setRegionCode(""); setStep(2); }}><SelectTrigger aria-label="국가 선택"><SelectValue placeholder="국가 선택" /></SelectTrigger><SelectContent>{TRIP_COUNTRY_OPTIONS.map((country) => <SelectItem key={country.code2} value={country.code2}>{country.name}</SelectItem>)}</SelectContent></Select>}
          {step === 2 && <Select value={regionCode} onValueChange={(value) => { setRegionCode(value); setStep(3); }} disabled={!countryCode}><SelectTrigger aria-label="여행 권역 선택"><SelectValue placeholder="여행 권역 선택" /></SelectTrigger><SelectContent>{regions.map((region) => <SelectItem key={region.code} value={region.code}>{region.label}</SelectItem>)}</SelectContent></Select>}
          {step === 3 && <div className="grid grid-cols-1 gap-4 sm:grid-cols-2"><div className="min-w-0 space-y-2"><Label htmlFor="ai-start">여행 시작일</Label><Input id="ai-start" type="date" required value={startDate} onChange={(event) => setStartDate(event.target.value)} /></div><div className="min-w-0 space-y-2"><Label htmlFor="ai-end">여행 종료일</Label><Input id="ai-end" type="date" required min={startDate || undefined} value={endDate} onChange={(event) => setEndDate(event.target.value)} /></div>{dateMessage && <p role="alert" className="sm:col-span-2 rounded-md border border-destructive/30 p-3 text-sm text-destructive">{dateMessage}</p>}</div>}
          {step === 4 && <Select value={companionType} onValueChange={(value) => { setCompanionType(value); setStep(5); }}><SelectTrigger aria-label="동행자 선택"><SelectValue placeholder="동행자 선택" /></SelectTrigger><SelectContent>{companionOptions.map((option) => <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>)}</SelectContent></Select>}
          {step === 5 && <fieldset className="space-y-3"><legend className="text-sm font-medium">여행 스타일 <span className="text-muted-foreground">({travelStyles.length}/3)</span></legend><div className="grid grid-cols-2 gap-x-3 gap-y-3 sm:grid-cols-3">{travelStyleOptions.map((option) => <div key={option.value} className="flex items-center gap-2"><Checkbox id={`ai-style-${option.value}`} checked={travelStyles.includes(option.value)} disabled={travelStyles.length === 3 && !travelStyles.includes(option.value)} onCheckedChange={(checked) => setTravelStyles((previous) => checked === true ? [...previous, option.value] : previous.filter((value) => value !== option.value))} /><Label htmlFor={`ai-style-${option.value}`}>{option.label}</Label></div>)}</div></fieldset>}
          {step === 6 && <div className="space-y-2"><Label htmlFor="ai-title">여행 이름 <span className="text-muted-foreground">(선택)</span></Label><Input id="ai-title" maxLength={80} placeholder="예: 도쿄 미식 여행" value={title} onChange={(event) => setTitle(event.target.value)} /><p className="text-sm text-muted-foreground">선택하지 않으면 AI가 여행지에 맞는 이름을 정해요.</p></div>}
        </div><div className={`flex flex-wrap ${step > 1 ? "justify-between" : "justify-end"} gap-2 border-t pt-4`}>{step > 1 && <Button type="button" variant="ghost" disabled={busy} onClick={() => { setValidationError(""); setStep((current) => Math.max(1, current - 1)); }}><ArrowLeft className="mr-2 h-4 w-4" />이전</Button>}{step < TOTAL_STEPS ? <Button type="button" onClick={next}>다음<ArrowRight className="ml-2 h-4 w-4" /></Button> : <div className="flex gap-2"><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>닫기</Button><Button type="submit" disabled={busy}><Sparkles className="mr-2 h-4 w-4" />일정 생성</Button></div>}</div></form>}
      </div>
    </DialogContent>
  </Dialog>;
}
