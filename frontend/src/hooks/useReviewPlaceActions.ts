import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAllItinerariesForTrip, itineraryApi } from "@/api/itinerary";
import type { PlaceSearchResult } from "@/api/places";
import { wishlistApi } from "@/api/wishlist";
import { readApiErrorMessage } from "@/api/http";
import { toast } from "@/hooks/use-toast";
import { tripQueryKeys } from "@/lib/tripQueryKeys";
import { runCreatePlaceItineraryFlow } from "@/lib/itineraryCreateFlow";

export function useReviewPlaceActions(tripId: number, open: boolean, tripStartDate?: string, tripEndDate?: string) {
  const queryClient = useQueryClient();
  const [addingPlace, setAddingPlace] = useState<PlaceSearchResult | null>(null);
  const [selectedVisitDay, setSelectedVisitDay] = useState("");
  const datedDays = useMemo(() => {
    if (!tripStartDate || !tripEndDate) return null;
    const count = Math.ceil((new Date(tripEndDate).getTime() - new Date(tripStartDate).getTime()) / 86400000) + 1;
    return Number.isFinite(count) && count > 0 ? Array.from({ length: count }, (_, index) => index + 1) : null;
  }, [tripStartDate, tripEndDate]);
  const itinerary = useQuery({
    queryKey: tripQueryKeys.itinerary(tripId),
    queryFn: () => fetchAllItinerariesForTrip(tripId),
    enabled: open && !!addingPlace && !datedDays,
  });
  const availableDays = datedDays ?? [...new Set((itinerary.data ?? []).map((item) => item.visitDay))].sort((a, b) => a - b);
  const cancelAdd = () => { setAddingPlace(null); setSelectedVisitDay(""); };
  const wishlist = useMutation({
    retry: false,
    mutationFn: (place: PlaceSearchResult) => wishlistApi.addWishlist(tripId, {
      externalPlaceId: place.externalPlaceId, placeName: place.placeName, address: place.address,
      latitude: place.latitude, longitude: place.longitude,
    }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: tripQueryKeys.wishlistRoot(tripId) });
      toast({ title: "위시리스트에 추가됨", description: "장소가 위시리스트에 추가되었습니다." });
    },
    onError: (error) => toast({ title: "추가 실패", description: readApiErrorMessage(error, "위시리스트 추가 중 오류가 발생했습니다."), variant: "destructive" }),
  });
  const addItinerary = useMutation({
    retry: false,
    mutationFn: ({ visitDay, placeId }: { visitDay: number; placeId?: number }) => runCreatePlaceItineraryFlow({
      visitDay, placeId,
      create: (day, data) => itineraryApi.createItinerary(tripId, day, { visitDay: day, ...data }),
      afterCreate: async () => {
        await queryClient.invalidateQueries({ queryKey: tripQueryKeys.itinerary(tripId), exact: true });
        await queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId) });
      },
      onSuccess: () => {
        toast({ title: "일정에 추가됨", description: "장소가 일정에 추가되었습니다." });
        cancelAdd();
      },
      onError: () => toast({ title: "추가 실패", description: "일정 추가 중 오류가 발생했습니다.", variant: "destructive" }),
    }),
  });
  return {
    addingPlace, selectedVisitDay, setSelectedVisitDay, availableDays, cancelAdd,
    startAdd: (place: PlaceSearchResult) => { setSelectedVisitDay(""); setAddingPlace(place); },
    addToWishlist: (place: PlaceSearchResult) => wishlist.mutate(place),
    confirmAdd: () => {
      if (!addingPlace || !availableDays.includes(Number(selectedVisitDay)) || addItinerary.isPending) return;
      addItinerary.mutate({ visitDay: Number(selectedVisitDay), placeId: addingPlace.placeId });
    },
    isAddingWishlist: wishlist.isPending, isAddingItinerary: addItinerary.isPending,
    daysError: !datedDays && itinerary.isError,
  };
}
