import test from "node:test";
import assert from "node:assert/strict";
import { aiTripCreateSchema, aiTripProposalSchema, aiTripAppliedSchema } from "./aiTripGeneration.ts";

const request = { regionCode: "KR_SEOUL", startDate: "2026-09-10", endDate: "2026-09-14", companionType: "SOLO", travelStyles: ["FOOD"] };
const proposal = {
  proposalId: 42, status: "READY", title: "서울 여행", summary: null, regionCode: "KR_SEOUL", country: "KR",
  startDate: "2026-09-10", endDate: "2026-09-10", companionType: "SOLO", travelStyles: ["FOOD"], createdTripId: null,
  warnings: ["영업시간 확인"], days: [{ visitDay: 1, theme: null, items: [{ order: 1, time: "09:00", placeName: "경복궁", searchQuery: "경복궁", memo: null, durationMinutes: null, styleTags: [] }] }],
};

test("generation allows inclusive one/five-day ranges across month boundaries", () => {
  assert.equal(aiTripCreateSchema.safeParse(request).success, true);
  assert.equal(aiTripCreateSchema.safeParse({ ...request, endDate: request.startDate }).success, true);
  assert.equal(aiTripCreateSchema.safeParse({ ...request, startDate: "2026-09-29", endDate: "2026-10-03" }).success, true);
});

test("generation rejects reversed, six-day and nonexistent calendar dates", () => {
  for (const dates of [
    { endDate: "2026-09-09" }, { endDate: "2026-09-15" },
    { startDate: "2026-02-30", endDate: "2026-03-02" }, { startDate: "not-a-date" },
  ]) assert.equal(aiTripCreateSchema.safeParse({ ...request, ...dates }).success, false);
});

test("generation enforces companion and unique 1-3 known styles", () => {
  for (const styles of [[], ["FOOD", "FOOD"], ["FOOD", "PHOTO", "LOCAL", "HEALING"], ["UNKNOWN"]]) {
    assert.equal(aiTripCreateSchema.safeParse({ ...request, travelStyles: styles }).success, false);
  }
  assert.equal(aiTripCreateSchema.safeParse({ ...request, companionType: "UNKNOWN" }).success, false);
  assert.equal(aiTripCreateSchema.safeParse({ ...request, regionCode: "  " }).success, false);
  assert.equal(aiTripCreateSchema.safeParse({ ...request, travelStyles: ["FOOD", "PHOTO", "LOCAL"] }).success, true);
});

test("proposal parsing preserves warnings, nullable fields and explicit unapplied state", () => {
  assert.deepEqual(aiTripProposalSchema.parse(proposal), proposal);
  assert.equal(aiTripProposalSchema.safeParse({ ...proposal, status: "APPLIED" }).success, false);
  assert.equal(aiTripProposalSchema.parse({ ...proposal, status: "APPLIED", createdTripId: 99 }).createdTripId, 99);
  assert.equal(aiTripProposalSchema.safeParse({ ...proposal, status: "UNKNOWN" }).success, false);
  assert.equal(aiTripProposalSchema.safeParse(null).success, false);
});

test("apply response requires an applied state and valid trip ID", () => {
  const result = { proposalId: 42, status: "APPLIED", tripId: 99, createdItemCount: 8, unresolvedPlaceCount: 2 };
  assert.deepEqual(aiTripAppliedSchema.parse(result), result);
  assert.equal(aiTripAppliedSchema.safeParse({ ...result, tripId: null }).success, false);
  assert.equal(aiTripAppliedSchema.safeParse({ ...result, status: "READY" }).success, false);
  assert.equal(aiTripAppliedSchema.safeParse({ ...result, unresolvedPlaceCount: -1 }).success, false);
});
