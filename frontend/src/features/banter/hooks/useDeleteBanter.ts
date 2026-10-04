import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { deleteBanter } from '../api/banterApi';
import type { BanterPage } from '../api/contracts';
import { banterQueryKeys } from '../api/queryKeys';
import { removeBanter } from './banterCache';

export function useDeleteBanter() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteBanter,
    onSuccess: (_result, banterId) => {
      queryClient.setQueriesData<InfiniteData<BanterPage>>(
        { queryKey: banterQueryKeys.lists },
        (data) => removeBanter(data, banterId),
      );
      void queryClient.invalidateQueries({ queryKey: banterQueryKeys.lists });
    },
  });
}
