import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteInvitePost } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';

export function useDeleteInvitePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteInvitePost,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });
    },
  });
}
