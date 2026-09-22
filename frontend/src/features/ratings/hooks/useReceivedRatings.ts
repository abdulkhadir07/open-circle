import { useInfiniteQuery } from '@tanstack/react-query';
import { getReceivedRatings } from '../api/ratingsApi';
import { ratingQueryKeys } from '../api/queryKeys';

const PAGE_SIZE = 20;

export function useReceivedRatings() {
  return useInfiniteQuery({
    queryKey: ratingQueryKeys.received,
    queryFn: ({ pageParam }) => getReceivedRatings({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page < lastPage.totalPages - 1 ? lastPage.page + 1 : undefined,
  });
}
