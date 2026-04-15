import type { PlacePhotoHint, PlaceTypeSummary } from "@/api/placeMetadata";

const OPENING_WARNING_LABELS: Record<string, string> = {
  OPEN: "영업 중",
  OUTSIDE_BUSINESS_HOURS: "영업시간 외",
  CLOSED_DAY: "휴무일",
  TEMPORARILY_CLOSED: "임시 휴무",
  NO_HOURS_INFO: "영업시간 정보 없음",
};

const PLACE_TYPE_LABELS: Record<string, string> = {
  restaurant: "식당",
  cafe: "카페",
  tourist_attraction: "관광지",
  lodging: "숙소",
  bar: "바",
  bakery: "베이커리",
  park: "공원",
  museum: "박물관",
  shopping_mall: "쇼핑",
  store: "상점",
  convenience_store: "편의점",
  subway_station: "지하철역",
  train_station: "기차역",
  airport: "공항",
  bus_station: "버스터미널",
  meal_takeaway: "포장",
  meal_delivery: "배달",
  night_club: "나이트라이프",
  aquarium: "아쿠아리움",
  amusement_park: "놀이공원",
  campground: "캠핑장",
  beach: "해변",
};

export const getPlaceTypeKey = (summary?: PlaceTypeSummary | null) =>
  summary?.primaryType || summary?.types.find((type) => type in PLACE_TYPE_LABELS) || null;

export const getPlaceTypeLabelFromKey = (key?: string | null) =>
  (key ? PLACE_TYPE_LABELS[key] : null) || key || null;

export const getPlaceTypeLabel = (summary?: PlaceTypeSummary | null) =>
  getPlaceTypeLabelFromKey(getPlaceTypeKey(summary)) || summary?.localizedPrimaryLabel || null;

export const getPlacePhotoUrl = (photoHint?: PlacePhotoHint | null) =>
  photoHint?.photoUri || photoHint?.photoUrl || photoHint?.imageUrl || null;

export const matchesPlaceTypeFilter = (
  summary: PlaceTypeSummary | null | undefined,
  filterKey: string,
) => {
  if (filterKey === "ALL") {
    return true;
  }

  return getPlaceTypeKey(summary) === filterKey;
};

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
