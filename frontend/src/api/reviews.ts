import { authenticatedAxios } from "@/api/auth";
import type { ApiResponse } from "@/api/http";
import type { PlaceSearchResult } from "@/api/places";

export interface RouteOptimizationItem {
  itemId: number;
  itemOrder: number;
  name: string;
  placeId: number | null;
  externalPlaceId: string | null;
}

export interface RouteOptimizationLeg {
  originItemId: number;
  destinationItemId: number;
  originName: string;
  destinationName: string;
  durationText: string | null;
  durationSeconds: number | null;
  distanceText: string | null;
  distanceMeters: number | null;
}

export interface RouteOptimizationDay {
  visitDay: number;
  currentOrder: RouteOptimizationItem[];
  optimizedOrder: RouteOptimizationItem[];
  currentDurationSeconds: number | null;
  optimizedDurationSeconds: number | null;
  savedDurationSeconds: number | null;
  currentLegs: RouteOptimizationLeg[];
  optimizedLegs: RouteOptimizationLeg[];
  warnings: string[];
}

export interface RouteOptimization {
  days: RouteOptimizationDay[];
}

export interface TripReview {
  reviewId: number;
  title?: string | null;
  concept: string | null;
  content: string;
  createdAt: string;
  recommendedPlaces: PlaceSearchResult[];
  routeOptimization?: RouteOptimization | null;
}

export interface CreateReviewRequest {
  concept?: string;
}

interface BackendReviewDetail {
  reviewId: number;
  title?: string | null;
  concept: string | null;
  content: string | null;
  createdAt: string | null;
  recommendedPlaces: PlaceSearchResult[];
  routeOptimization?: RouteOptimization | null;
}

interface BackendReviewSummary {
  reviewId: number;
  title?: string | null;
  concept: string | null;
  createdAt: string | null;
}

const toTripReview = (review: BackendReviewDetail | BackendReviewSummary): TripReview => ({
  reviewId: review.reviewId,
  title: review.title ?? null,
  concept: review.concept ?? null,
  content: "content" in review ? review.content || "" : "",
  createdAt: review.createdAt || "",
  recommendedPlaces: "recommendedPlaces" in review ? review.recommendedPlaces || [] : [],
  routeOptimization: "routeOptimization" in review ? review.routeOptimization ?? null : null,
});

export const reviewApi = {
  createReview: async (tripId: number, request: CreateReviewRequest): Promise<TripReview> => {
    const response = await authenticatedAxios.post<ApiResponse<BackendReviewDetail>>(`/trips/${tripId}/reviews`, request, {
      headers: {
        "Content-Type": "application/json",
      },
    });
    return toTripReview(response.data.data as BackendReviewDetail);
  },

  getAllReviews: async (tripId: number, page = 0, size = 10): Promise<TripReview[]> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendReviewSummary[]>>(`/trips/${tripId}/reviews`, {
      params: { page, size },
    });
    return (response.data.data ?? []).map((review) => toTripReview(review));
  },

  getReview: async (tripId: number, reviewId: number): Promise<TripReview> => {
    const response = await authenticatedAxios.get<ApiResponse<BackendReviewDetail>>(`/trips/${tripId}/reviews/${reviewId}`);
    return toTripReview(response.data.data as BackendReviewDetail);
  },
};
