import { useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAllItinerariesForTrip, itineraryApi } from "@/api/itinerary";
import type { RouteOptimization } from "@/api/reviews";
import { applyFreshRouteOptimization, buildRouteOptimizationApply } from "@/lib/routeOptimizationApply";
import { tripQueryKeys } from "@/lib/tripQueryKeys";

export function useApplyRouteOptimization(tripId: number, open: boolean, optimization: RouteOptimization | null | undefined) {
  const queryClient = useQueryClient();
  const submitting = useRef(false);
  const itinerary = useQuery({
    queryKey: tripQueryKeys.itinerary(tripId),
    queryFn: () => fetchAllItinerariesForTrip(tripId),
    enabled: open && !!optimization,
  });
  const mutation = useMutation({
    retry: false,
    mutationFn: (optimization: RouteOptimization) => applyFreshRouteOptimization(optimization, {
      // Call the API directly, never fetchQuery/ensureQueryData against a potentially fresh cache.
      fetchItinerary: () => fetchAllItinerariesForTrip(tripId),
      reorder: (request) => itineraryApi.updateItineraryOrder(tripId, request),
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: tripQueryKeys.itinerary(tripId), exact: true });
      await queryClient.invalidateQueries({ queryKey: tripQueryKeys.directions(tripId) });
    },
  });

  const apply = async (optimization: RouteOptimization): Promise<boolean> => {
    if (submitting.current) return false;
    submitting.current = true;
    try {
      await mutation.mutateAsync(optimization);
      return true;
    } catch {
      return false;
    } finally {
      submitting.current = false;
    }
  };
  return { apply, isPending: mutation.isPending, error: mutation.error, itinerary,
    guard: buildRouteOptimizationApply(optimization, itinerary.data) };
}
