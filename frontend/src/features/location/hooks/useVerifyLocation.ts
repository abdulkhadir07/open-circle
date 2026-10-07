import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { invitePostQueryKeys } from '@/features/invite-posts/api/queryKeys';
import { verifyLocation } from '../api/locationApi';

export function useVerifyLocation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: verifyLocation,
    onSuccess: (user) => {
      queryClient.setQueryData(authQueryKeys.currentUser, user);
      // A new place means different invites are nearby.
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });
    },
  });
}
