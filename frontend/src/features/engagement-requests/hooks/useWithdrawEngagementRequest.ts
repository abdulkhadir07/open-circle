import { useMutation, useQueryClient } from '@tanstack/react-query';
import { withdrawEngagementRequest } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useWithdrawEngagementRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: withdrawEngagementRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: engagementRequestQueryKeys.mine });
    },
  });
}
