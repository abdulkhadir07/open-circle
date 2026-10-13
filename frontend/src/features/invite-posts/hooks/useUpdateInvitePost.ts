import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateInvitePost } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';

export function useUpdateInvitePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateInvitePost,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });
    },
  });
}
