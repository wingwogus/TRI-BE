import type { NormalizedPlaceCategoryKey, PlacePhotoHint, PlaceTypeSummary } from "@/api/placeMetadata";

const OPENING_WARNING_LABELS: Record<string, string> = {
  OPEN: "영업 중",
  OUTSIDE_BUSINESS_HOURS: "영업시간 외",
  CLOSED_DAY: "휴무일",
  TEMPORARILY_CLOSED: "임시 휴무",
  NO_HOURS_INFO: "영업시간 정보 없음",
};

const NORMALIZED_CATEGORY_LABELS: Record<NormalizedPlaceCategoryKey, string> = {
  KOREAN_FOOD: "한식",
  JAPANESE_FOOD: "일식",
  CHINESE_FOOD: "중식",
  RESTAURANT: "식당",
  CAFE: "카페",
  BAKERY: "베이커리",
  BAR: "바",
  ATTRACTION: "관광지",
  SHOPPING: "쇼핑",
  STAY: "숙소",
  PARK: "공원",
  MUSEUM: "박물관",
  TRANSPORT: "교통",
  ETC: "기타",
};

const PLACE_TYPE_FALLBACK_LABELS: Record<string, string> = {
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
};

export const getPlaceTypeKey = (
  summary?: PlaceTypeSummary | null,
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null,
) =>
  normalizedCategoryKey
  || summary?.types.find((type) => type in PLACE_TYPE_FALLBACK_LABELS)
  || (summary?.primaryType && summary.primaryType in PLACE_TYPE_FALLBACK_LABELS ? summary.primaryType : null)
  || summary?.primaryType
  || null;

export const getPlaceTypeLabelFromKey = (key?: string | null) =>
  (key ? NORMALIZED_CATEGORY_LABELS[key as NormalizedPlaceCategoryKey] : null)
  || (key ? PLACE_TYPE_FALLBACK_LABELS[key] : null)
  || key
  || null;

export const getPlaceTypeLabel = (
  summary?: PlaceTypeSummary | null,
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null,
) =>
  getPlaceTypeLabelFromKey(getPlaceTypeKey(summary, normalizedCategoryKey)) || summary?.localizedPrimaryLabel || null;

export const getPlacePhotoUrl = (_photoHint?: PlacePhotoHint | null) => null;

export const matchesPlaceTypeFilter = (
  summary: PlaceTypeSummary | null | undefined,
  filterKey: string,
  normalizedCategoryKey?: NormalizedPlaceCategoryKey | null,
) => {
  if (filterKey === "ALL") {
    return true;
  }

  return getPlaceTypeKey(summary, normalizedCategoryKey) === filterKey;
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
