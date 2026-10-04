import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { likeBanter, unlikeBanter } from '../api/banterApi';
import type { BanterPage } from '../api/contracts';
import { banterQueryKeys } from '../api/queryKeys';
import { applyLike, setLikeState } from './banterCache';

type ToggleVariables = { banterId: string; like: boolean };
type CachedLists = [readonly unknown[], InfiniteData<BanterPage> | undefined][];

/**
 * Likes or unlikes a banter. The heart and count flip immediately (optimistic), roll back if the
 * request fails, and settle on the server's numbers once it answers.
 */
export function useToggleBanterLike() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ banterId, like }: ToggleVariables) =>
      like ? likeBanter(banterId) : unlikeBanter(banterId),
    onMutate: async ({ banterId, like }) => {
      await queryClient.cancelQueries({ queryKey: banterQueryKeys.lists });
      const previous: CachedLists = queryClient.getQueriesData<InfiniteData<BanterPage>>({
        queryKey: banterQueryKeys.lists,
      });
      queryClient.setQueriesData<InfiniteData<BanterPage>>(
        { queryKey: banterQueryKeys.lists },
        (data) => applyLike(data, banterId, like),
      );
      return { previous };
    },
    onError: (_error, _variables, context) => {
      for (const [key, data] of context?.previous ?? []) {
        queryClient.setQueryData(key, data);
      }
    },
    onSuccess: (state, { banterId }) => {
      queryClient.setQueriesData<InfiniteData<BanterPage>>(
        { queryKey: banterQueryKeys.lists },
        (data) => setLikeState(data, banterId, state),
      );
    },
  });
}
