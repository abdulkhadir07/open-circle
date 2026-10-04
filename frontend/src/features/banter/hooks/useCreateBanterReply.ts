import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { createBanterReply } from '../api/banterApi';
import type { BanterPage } from '../api/contracts';
import { banterQueryKeys } from '../api/queryKeys';
import { adjustReplyCount } from './banterCache';

export function useCreateBanterReply() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createBanterReply,
    onSuccess: (_reply, { banterId }) => {
      queryClient.setQueriesData<InfiniteData<BanterPage>>(
        { queryKey: banterQueryKeys.lists },
        (data) => adjustReplyCount(data, banterId, 1),
      );
      void queryClient.invalidateQueries({ queryKey: banterQueryKeys.replies(banterId) });
    },
  });
}
