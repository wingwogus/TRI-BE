import { z } from "zod";

export const companionOptions = [
  { value: "SOLO", label: "혼자" },
  { value: "COUPLE", label: "연인" },
  { value: "FRIENDS", label: "친구" },
  { value: "FAMILY", label: "가족" },
  { value: "PARENTS", label: "부모님과" },
  { value: "CHILDREN", label: "아이와" },
] as const;

export const travelStyleOptions = [
  { value: "FOOD", label: "맛집" },
  { value: "HEALING", label: "휴식" },
  { value: "ACTIVITY", label: "액티비티" },
  { value: "CULTURE", label: "문화" },
  { value: "SHOPPING", label: "쇼핑" },
  { value: "NATURE", label: "자연" },
  { value: "PHOTO", label: "사진" },
  { value: "LOCAL", label: "현지 체험" },
  { value: "LUXURY", label: "럭셔리" },
  { value: "BUDGET", label: "알뜰 여행" },
] as const;

const companionSchema = z.enum(["SOLO", "COUPLE", "FRIENDS", "FAMILY", "PARENTS", "CHILDREN"]);
const styleSchema = z.enum(["FOOD", "HEALING", "ACTIVITY", "CULTURE", "SHOPPING", "NATURE", "PHOTO", "LOCAL", "LUXURY", "BUDGET"]);
const dateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "날짜를 선택해주세요.").refine((value) => {
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}, "올바른 날짜를 선택해주세요.");

export const aiTripCreateSchema = z.object({
  regionCode: z.string().trim().min(1, "여행 권역을 선택해주세요."),
  startDate: dateSchema,
  endDate: dateSchema,
  title: z.string().trim().max(80, "여행 이름은 80자 이내로 입력해주세요.").optional(),
  companionType: companionSchema,
  travelStyles: z.array(styleSchema).min(1, "여행 스타일을 하나 이상 선택해주세요.").max(3, "여행 스타일은 최대 3개까지 선택할 수 있습니다.")
    .refine((values) => new Set(values).size === values.length, "중복된 여행 스타일은 선택할 수 없습니다."),
}).superRefine((value, context) => {
  const nights = (Date.parse(`${value.endDate}T00:00:00Z`) - Date.parse(`${value.startDate}T00:00:00Z`)) / 86_400_000;
  if (nights < 0) context.addIssue({ code: "custom", path: ["endDate"], message: "종료일은 시작일보다 이전일 수 없습니다." });
  if (nights > 4) context.addIssue({ code: "custom", path: ["endDate"], message: "AI 일정은 최대 4박 5일까지 만들 수 있습니다." });
});

const positiveId = z.number().int().positive().safe();
export const aiTripProposalSchema = z.object({
  proposalId: positiveId,
  status: z.enum(["READY", "APPLIED", "FAILED"]),
  title: z.string(),
  summary: z.string().nullable(),
  regionCode: z.string(),
  country: z.string(),
  startDate: dateSchema,
  endDate: dateSchema,
  companionType: companionSchema,
  travelStyles: z.array(styleSchema),
  createdTripId: positiveId.nullable(),
  warnings: z.array(z.string()),
  days: z.array(z.object({
    visitDay: z.number().int().positive(),
    theme: z.string().nullable(),
    items: z.array(z.object({
      order: z.number().int().positive(),
      time: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/),
      placeName: z.string(),
      searchQuery: z.string(),
      memo: z.string().nullable(),
      durationMinutes: z.number().int().nullable(),
      styleTags: z.array(z.string()),
    })),
  })).min(1).max(6),
}).refine((proposal) => proposal.status !== "APPLIED" || proposal.createdTripId !== null, "적용된 여행 정보를 확인할 수 없습니다.");

export const aiTripAppliedSchema = z.object({
  proposalId: positiveId,
  status: z.literal("APPLIED"),
  tripId: positiveId,
  createdItemCount: z.number().int().nonnegative(),
  unresolvedPlaceCount: z.number().int().nonnegative(),
});

export type AiTripCreateRequest = z.infer<typeof aiTripCreateSchema>;
export type AiTripProposal = z.infer<typeof aiTripProposalSchema>;
export type AiTripApplied = z.infer<typeof aiTripAppliedSchema>;
export type TripCompanionType = z.infer<typeof companionSchema>;
export type TripTravelStyle = z.infer<typeof styleSchema>;
