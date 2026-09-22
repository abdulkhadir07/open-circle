import { useQueryClient } from '@tanstack/react-query';
import type { InfiniteData } from '@tanstack/react-query';
import { useEffect } from 'react';
import { stompClient } from '@/lib/realtime/stompClient';
import { parseNotificationEvent, type NotificationInbox } from '../api/contracts';
import { notificationQueryKeys } from '../api/queryKeys';
import { playNotificationSound } from '../lib/playNotificationSound';
import { upsertNotification } from './notificationCache';

export function useNotificationsRealtime() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const unsubscribeQueue = stompClient.subscribe('/user/queue/notifications', (message) => {
      try {
        const { notification, unreadCount } = parseNotificationEvent(JSON.parse(message.body));
        queryClient.setQueryData(
          notificationQueryKeys.list,
          (existing: InfiniteData<NotificationInbox> | undefined) =>
            existing
              ? { ...existing, pages: upsertNotification(existing.pages, notification) }
              : existing,
        );
        queryClient.setQueryData(notificationQueryKeys.unreadCount, unreadCount);
        playNotificationSound();
      } catch (error) {
        console.error('Failed to process realtime notification', error);
      }
    });

    const unsubscribeReconnect = stompClient.onReconnect(() => {
      void queryClient.invalidateQueries({ queryKey: notificationQueryKeys.list });
      void queryClient.invalidateQueries({ queryKey: notificationQueryKeys.unreadCount });
    });

    return () => {
      unsubscribeQueue();
      unsubscribeReconnect();
    };
  }, [queryClient]);
}
