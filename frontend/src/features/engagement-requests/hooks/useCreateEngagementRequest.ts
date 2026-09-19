import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createEngagementRequest } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useCreateEngagementRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createEngagementRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: engagementRequestQueryKeys.mine });
    },
  });
}
