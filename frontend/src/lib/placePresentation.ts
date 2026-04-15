import type { PlacePhotoHint, PlaceTypeSummary } from "@/api/placeMetadata";

const OPENING_WARNING_LABELS: Record<string, string> = {
  OPEN: "영업 중",
  OUTSIDE_BUSINESS_HOURS: "영업시간 외",
  CLOSED_DAY: "휴무일",
  TEMPORARILY_CLOSED: "임시 휴무",
  NO_HOURS_INFO: "영업시간 정보 없음",
};

export const getPlaceTypeLabel = (summary?: PlaceTypeSummary | null) =>
  summary?.localizedPrimaryLabel || summary?.primaryType || null;

export const getPlacePhotoUrl = (photoHint?: PlacePhotoHint | null) =>
  photoHint?.photoUri || photoHint?.photoUrl || photoHint?.imageUrl || null;

export const getOpeningStatusLabel = (warning?: string | null) => {
  if (!warning) {
    return null;
  }

  return OPENING_WARNING_LABELS[warning] || warning;
};

export const getOpeningStatusTone = (warning?: string | null) => {
  switch (warning) {
    case "OPEN":
      return "default";
    case "OUTSIDE_BUSINESS_HOURS":
    case "CLOSED_DAY":
    case "TEMPORARILY_CLOSED":
      return "destructive";
    case "NO_HOURS_INFO":
      return "secondary";
    default:
      return "secondary";
  }
};
