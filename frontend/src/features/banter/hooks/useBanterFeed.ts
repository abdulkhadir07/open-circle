import { useInfiniteQuery } from '@tanstack/react-query';
import { getBanterBoard } from '../api/banterApi';
import type { BanterSort } from '../api/contracts';
import { banterQueryKeys } from '../api/queryKeys';

const PAGE_SIZE = 20;

export function useBanterFeed(sort: BanterSort) {
  return useInfiniteQuery({
    queryKey: banterQueryKeys.list(sort),
    queryFn: ({ pageParam }) => getBanterBoard({ sort, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page < lastPage.totalPages - 1 ? lastPage.page + 1 : undefined,
  });
}
