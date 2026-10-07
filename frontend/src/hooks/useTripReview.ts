import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reviewApi, type CreateReviewRequest, type TripReview } from "@/api/reviews";
import { tripQueryKeys } from "@/lib/tripQueryKeys";
import { toast } from "@/hooks/use-toast";

export function useTripReview(tripId: number, open: boolean, activeTab: string, selectedReviewId: number | null) {
  const queryClient = useQueryClient();
  const [newReview, setNewReview] = useState<TripReview | null>(null);
  const creating = useRef(false);
  const reviews = useQuery({
    queryKey: tripQueryKeys.reviews(tripId),
    queryFn: () => reviewApi.getAllReviews(tripId),
    enabled: open && activeTab === "history" && selectedReviewId === null && !newReview,
  });
  const detail = useQuery({
    queryKey: tripQueryKeys.review(tripId, selectedReviewId),
    queryFn: () => reviewApi.getReview(tripId, selectedReviewId!),
    enabled: open && activeTab === "history" && selectedReviewId !== null && !newReview,
  });
  const createMutation = useMutation({
    mutationFn: (request: CreateReviewRequest) => reviewApi.createReview(tripId, request),
    retry: false,
    onSuccess: (data) => {
      setNewReview(data);
      queryClient.setQueryData(tripQueryKeys.review(tripId, data.reviewId), data);
      void queryClient.invalidateQueries({ queryKey: tripQueryKeys.reviews(tripId), exact: true, refetchType: "none" });
      toast({ title: "AI 리뷰 완료", description: "여행 일정에 대한 AI 분석이 완료되었습니다." });
    },
    onError: () => toast({ title: "리뷰 생성 실패", description: "AI 리뷰를 생성하는 중 오류가 발생했습니다.", variant: "destructive" }),
  });
  const createReview = async (concept: string) => {
    if (creating.current) return;
    creating.current = true;
    setNewReview(null);
    try {
      await createMutation.mutateAsync({ concept: concept.trim() || undefined });
    } catch {
      // The mutation exposes the error and displays the existing toast.
    } finally {
      creating.current = false;
    }
  };
  return { reviews, detail, newReview, clearNewReview: () => setNewReview(null), createReview, isCreating: createMutation.isPending };
}
