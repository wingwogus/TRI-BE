export interface PlaceTypeSummary {
  primaryType: string | null;
  types: string[];
  localizedPrimaryLabel: string | null;
}

export interface PlacePhotoHint {
  photoUri?: string | null;
  photoUrl?: string | null;
  imageUrl?: string | null;
  widthPx?: number | null;
  heightPx?: number | null;
  authorAttributions?: string[];
}

export interface PlaceDetailSummary {
  businessStatus?: string | null;
  rating?: number | null;
  userRatingCount?: number | null;
  editorialSummary?: string | null;
}
