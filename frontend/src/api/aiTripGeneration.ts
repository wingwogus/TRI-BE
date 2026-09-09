import { authenticatedAxios, type ApiResponse } from "@/api/http";
import { aiTripProposalSchema, aiTripAppliedSchema, type AiTripCreateRequest } from "@/lib/aiTripGeneration";

const basePath = "/trips/ai-generations";

export const aiTripGenerationApi = {
  async create(request: AiTripCreateRequest) {
    const response = await authenticatedAxios.post<ApiResponse<unknown>>(basePath, request);
    return aiTripProposalSchema.parse(response.data.data);
  },
  async get(proposalId: number, signal?: AbortSignal) {
    const response = await authenticatedAxios.get<ApiResponse<unknown>>(`${basePath}/${proposalId}`, { signal });
    return aiTripProposalSchema.parse(response.data.data);
  },
  async apply(proposalId: number, allowTextOnlyItems: boolean) {
    const response = await authenticatedAxios.post<ApiResponse<unknown>>(`${basePath}/${proposalId}/apply`, { allowTextOnlyItems });
    return aiTripAppliedSchema.parse(response.data.data);
  },
};
