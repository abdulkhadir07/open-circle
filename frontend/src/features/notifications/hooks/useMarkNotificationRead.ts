import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InfiniteData } from '@tanstack/react-query';
import { markNotificationRead } from '../api/notificationsApi';
import { notificationQueryKeys } from '../api/queryKeys';
import type { NotificationInbox } from '../api/contracts';
import { markOneRead } from './notificationCache';

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: (_data, notificationId) => {
      const existingList = queryClient.getQueryData<InfiniteData<NotificationInbox>>(
        notificationQueryKeys.list,
      );
      const wasUnread =
        existingList?.pages
          .flatMap((page) => page.notifications)
          .some((notification) => notification.id === notificationId && !notification.read) ??
        false;

      queryClient.setQueryData(
        notificationQueryKeys.list,
        (existing: InfiniteData<NotificationInbox> | undefined) =>
          existing ? { ...existing, pages: markOneRead(existing.pages, notificationId) } : existing,
      );

      if (wasUnread) {
        queryClient.setQueryData(notificationQueryKeys.unreadCount, (count: number | undefined) =>
          count !== undefined ? Math.max(0, count - 1) : count,
        );
      }
    },
  });
}
