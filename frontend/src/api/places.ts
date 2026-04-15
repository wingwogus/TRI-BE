import {authenticatedAxios} from './auth';
import type { PlaceDetailSummary, PlacePhotoHint, PlaceTypeSummary } from "@/api/placeMetadata";

// Backend response types matching PlaceDto
export interface PlaceSearchResult {
  placeId?: number;
  externalPlaceId: string;
  placeName: string;
  address: string;
  latitude: number;
  longitude: number;
  placeTypeSummary?: PlaceTypeSummary | null;
  photoHint?: PlacePhotoHint | null;
  placeDetailSummary?: PlaceDetailSummary | null;
}

// API functions
export const placesApi = {
  // Search places using Google Maps API
  searchPlaces: async (
    query?: string,
    region?: string,
    latitude?: number,
    longitude?: number,
    radiusMeters?: number,
    regionContextKey?: string,
    language: string = 'ko'
  ): Promise<PlaceSearchResult[]> => {
    const response = await authenticatedAxios.get<{ data: PlaceSearchResult[] }>(
      '/places/search',
      { 
        params: { query, region, latitude, longitude, radiusMeters, regionContextKey, language } 
      }
    );
    return response.data.data;
  }
};
