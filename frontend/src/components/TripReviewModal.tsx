import { useState } from "react";
import { ArrowRight, Calendar, History, Loader2, Plus, Sparkles } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { TripReviewContent } from "@/components/TripReviewContent";
import { useTripReview } from "@/hooks/useTripReview";
import { useReviewPlaceActions } from "@/hooks/useReviewPlaceActions";

interface TripReviewModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tripId: number;
  tripStartDate?: string;
  tripEndDate?: string;
}

export const TripReviewModal = ({ open, onOpenChange, tripId, tripStartDate, tripEndDate }: TripReviewModalProps) => {
  const [concept, setConcept] = useState("");
  const [activeTab, setActiveTab] = useState("new");
  const [selectedReviewId, setSelectedReviewId] = useState<number | null>(null);
  const review = useTripReview(tripId, open, activeTab, selectedReviewId);
  const places = useReviewPlaceActions(tripId, open, tripStartDate, tripEndDate);
  const displayedReview = review.newReview ?? (selectedReviewId !== null ? review.detail.data : null);
  const backToList = () => { review.clearNewReview(); setSelectedReviewId(null); setActiveTab("history"); };
  const startNew = () => { review.clearNewReview(); setSelectedReviewId(null); setConcept(""); setActiveTab("new"); };
  const close = () => {
    startNew();
    places.cancelAdd();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => { if (!nextOpen) close(); }}>
      <DialogContent className="max-w-4xl max-h-[85vh] overflow-y-auto">
        <DialogHeader className="space-y-3 pb-4">
          <DialogTitle className="flex items-center gap-3 text-xl"><Sparkles className="h-6 w-6 shrink-0 text-primary" />AI 여행 일정 분석</DialogTitle>
          <DialogDescription>현재 일정과 여행 콘셉트에 대한 AI 분석</DialogDescription>
        </DialogHeader>
        {displayedReview ? (
          <div className="min-w-0 space-y-6">
            <TripReviewContent review={displayedReview} tripId={tripId} open={open}
              onAddWishlist={places.addToWishlist} onAddItinerary={places.startAdd}
              isAddingWishlist={places.isAddingWishlist} isAddingItinerary={places.isAddingItinerary} />
            <div className="flex flex-wrap gap-3 border-t pt-4">
              <Button variant="outline" onClick={backToList} className="flex-1"><History className="mr-2 h-4 w-4" />목록으로</Button>
              <Button onClick={startNew} className="flex-1"><Sparkles className="mr-2 h-4 w-4" />새로운 분석 받기</Button>
            </div>
          </div>
        ) : selectedReviewId !== null ? (
          <div className="space-y-4 py-8">
            {review.detail.isError ? <p role="alert" className="text-sm text-destructive">리뷰를 불러오지 못했습니다.</p> :
              <Loader2 aria-label="리뷰를 불러오는 중" className="mx-auto h-8 w-8 animate-spin text-primary" />}
            {review.detail.isError && <Button variant="outline" onClick={() => void review.detail.refetch()}>다시 시도</Button>}
            <Button variant="outline" onClick={backToList}><History className="mr-2 h-4 w-4" />목록으로</Button>
          </div>
        ) : (
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="new"><Plus className="mr-2 h-4 w-4" />새 분석</TabsTrigger>
              <TabsTrigger value="history"><History className="mr-2 h-4 w-4" />이전 분석</TabsTrigger>
            </TabsList>
            <TabsContent value="new" className="mt-6">
              <form className="space-y-6" onSubmit={(event) => { event.preventDefault(); void review.createReview(concept); }}>
                <div className="space-y-3">
                  <Label htmlFor="concept">여행 콘셉트 (선택사항)</Label>
                  <Input id="concept" value={concept} onChange={(event) => setConcept(event.target.value)}
                    placeholder="예: 혼자 식도락 여행, 가족 힐링 여행" disabled={review.isCreating} />
                </div>
                <Button type="submit" disabled={review.isCreating} className="w-full">
                  {review.isCreating ? <Loader2 className="mr-2 h-5 w-5 animate-spin" /> : <Sparkles className="mr-2 h-5 w-5" />}
                  {review.isCreating ? "AI 분석 중..." : "AI 리뷰 받기"}
                </Button>
              </form>
            </TabsContent>
            <TabsContent value="history" className="mt-6 space-y-3">
              {review.reviews.isLoading ? <Loader2 aria-label="리뷰 목록을 불러오는 중" className="mx-auto my-8 h-8 w-8 animate-spin text-primary" /> :
                review.reviews.isError ? <div className="space-y-3"><p role="alert" className="text-sm text-destructive">리뷰 목록을 불러오지 못했습니다.</p>
                  <Button variant="outline" onClick={() => void review.reviews.refetch()}>다시 시도</Button></div> :
                  !review.reviews.data?.length ? <p className="py-8 text-center text-muted-foreground">아직 생성된 리뷰가 없습니다.</p> :
                    review.reviews.data.map((entry) => (
                      <button key={entry.reviewId} type="button" onClick={() => setSelectedReviewId(entry.reviewId)}
                        className="flex w-full items-start gap-3 rounded-lg border p-4 text-left hover:bg-muted focus-visible:outline focus-visible:outline-2">
                        <Sparkles className="mt-1 h-5 w-5 shrink-0 text-primary" />
                        <span className="min-w-0 flex-1 space-y-2">
                          <span className="block font-medium break-words">{entry.title || entry.concept || "여행 일정 분석"}</span>
                          <span className="flex items-center gap-2 text-sm text-muted-foreground"><Calendar className="h-3.5 w-3.5 shrink-0" />
                            {entry.createdAt ? new Date(entry.createdAt).toLocaleString("ko-KR") : "날짜 정보 없음"}</span>
                        </span>
                        <ArrowRight className="mt-1 h-5 w-5 shrink-0 text-muted-foreground" />
                      </button>
                    ))}
            </TabsContent>
          </Tabs>
        )}
        <Dialog open={open && !!places.addingPlace} onOpenChange={(nextOpen) => { if (!nextOpen) places.cancelAdd(); }}>
          <DialogContent>
            <DialogHeader><DialogTitle>일정에 추가</DialogTitle><DialogDescription>{places.addingPlace?.placeName}</DialogDescription></DialogHeader>
            <div className="space-y-2 py-4">
              <Label htmlFor="review-visit-day">날짜 선택</Label>
              <Select value={places.selectedVisitDay} onValueChange={places.setSelectedVisitDay}>
                <SelectTrigger id="review-visit-day"><SelectValue placeholder="날짜를 선택하세요" /></SelectTrigger>
                <SelectContent className="max-h-48 overflow-y-auto">{places.availableDays.map((day) => <SelectItem key={day} value={String(day)}>Day {day}</SelectItem>)}</SelectContent>
              </Select>
              {places.daysError && <p role="alert" className="text-sm text-destructive">일정 날짜를 불러오지 못했습니다.</p>}
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={places.cancelAdd}>취소</Button>
              <Button onClick={places.confirmAdd} disabled={!places.selectedVisitDay || places.isAddingItinerary}>
                {places.isAddingItinerary ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Plus className="mr-2 h-4 w-4" />}
                {places.isAddingItinerary ? "추가 중..." : "추가"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
};
