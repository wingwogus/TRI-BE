import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { aiTripGenerationApi } from "@/api/aiTripGeneration";
import { aiTripCreateSchema, type AiTripCreateRequest, type AiTripProposal } from "@/lib/aiTripGeneration";
import { tripQueryKeys } from "@/lib/tripQueryKeys";

export function useAiTripGeneration(open: boolean) {
  const queryClient = useQueryClient();
  const [proposalId, setProposalId] = useState<number | null>(null);
  const inFlight = useRef(false);
  const proposal = useQuery({
    queryKey: tripQueryKeys.aiTripProposal(proposalId),
    queryFn: ({ signal }) => aiTripGenerationApi.get(proposalId!, signal),
    enabled: open && !!proposalId,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  const createMutation = useMutation({
    mutationFn: (request: AiTripCreateRequest) => aiTripGenerationApi.create(aiTripCreateSchema.parse(request)),
    retry: false,
    onSuccess: (result) => {
      queryClient.setQueryData(tripQueryKeys.aiTripProposal(result.proposalId), result);
      setProposalId(result.proposalId);
    },
  });

  const applyMutation = useMutation({
    mutationFn: (allowTextOnlyItems: boolean) => aiTripGenerationApi.apply(proposalId!, allowTextOnlyItems),
    retry: false,
    onSuccess: (result) => {
      queryClient.setQueryData<AiTripProposal>(tripQueryKeys.aiTripProposal(result.proposalId), (previous) =>
        previous ? { ...previous, status: "APPLIED", createdTripId: result.tripId } : previous,
      );
      void queryClient.invalidateQueries({ queryKey: ["trips"] });
    },
  });

  const generate = async (request: AiTripCreateRequest) => {
    if (inFlight.current) return null;
    inFlight.current = true;
    applyMutation.reset();
    try {
      return await createMutation.mutateAsync(request);
    } finally {
      inFlight.current = false;
    }
  };

  const apply = async (allowTextOnlyItems: boolean) => {
    if (inFlight.current || !proposalId || proposal.data?.status !== "READY") return null;
    inFlight.current = true;
    try {
      return await applyMutation.mutateAsync(allowTextOnlyItems);
    } finally {
      inFlight.current = false;
    }
  };

  const startOver = () => {
    if (inFlight.current) return;
    setProposalId(null);
    createMutation.reset();
    applyMutation.reset();
  };

  return {
    proposalId, proposal: proposal.data, isFetching: proposal.isFetching,
    isGenerating: createMutation.isPending, isApplying: applyMutation.isPending,
    error: applyMutation.error ?? createMutation.error ?? proposal.error,
    generate, apply, startOver, reload: proposal.refetch,
  };
}
