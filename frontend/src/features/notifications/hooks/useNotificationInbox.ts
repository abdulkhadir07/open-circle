import { useInfiniteQuery } from '@tanstack/react-query';
import { getNotificationInbox } from '../api/notificationsApi';
import { notificationQueryKeys } from '../api/queryKeys';

const PAGE_SIZE = 20;

export function useNotificationInbox() {
  return useInfiniteQuery({
    queryKey: notificationQueryKeys.list,
    queryFn: ({ pageParam }) => getNotificationInbox({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page < lastPage.totalPages - 1 ? lastPage.page + 1 : undefined,
  });
}
