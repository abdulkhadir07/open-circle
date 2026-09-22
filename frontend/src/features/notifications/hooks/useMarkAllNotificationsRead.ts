import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { InfiniteData } from '@tanstack/react-query';
import { markAllNotificationsRead } from '../api/notificationsApi';
import { notificationQueryKeys } from '../api/queryKeys';
import type { NotificationInbox } from '../api/contracts';
import { markAllRead } from './notificationCache';

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.setQueryData(
        notificationQueryKeys.list,
        (existing: InfiniteData<NotificationInbox> | undefined) =>
          existing ? { ...existing, pages: markAllRead(existing.pages) } : existing,
      );
      queryClient.setQueryData(notificationQueryKeys.unreadCount, 0);
    },
  });
}
