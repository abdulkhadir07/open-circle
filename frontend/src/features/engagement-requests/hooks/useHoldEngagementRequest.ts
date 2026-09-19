import { useMutation, useQueryClient } from '@tanstack/react-query';
import { holdEngagementRequest } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useHoldEngagementRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: holdEngagementRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: engagementRequestQueryKeys.received });
    },
  });
}
