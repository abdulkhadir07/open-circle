import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createInvitePost } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';

export function useCreateInvitePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createInvitePost,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });
    },
  });
}
