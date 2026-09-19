import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invitePostQueryKeys } from '@/features/invite-posts/api/queryKeys';
import { acceptEngagementRequest } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

/** Accepting also consumes a capacity slot on the post, so the feed needs refreshing too. */
export function useAcceptEngagementRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: acceptEngagementRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: engagementRequestQueryKeys.received });
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });
    },
  });
}
