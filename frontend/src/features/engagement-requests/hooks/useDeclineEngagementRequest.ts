import { useMutation, useQueryClient } from '@tanstack/react-query';
import { declineEngagementRequest } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useDeclineEngagementRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: declineEngagementRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: engagementRequestQueryKeys.received });
    },
  });
}
