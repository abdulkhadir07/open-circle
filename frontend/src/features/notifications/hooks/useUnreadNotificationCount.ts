import { useQuery } from '@tanstack/react-query';
import { getUnreadNotificationCount } from '../api/notificationsApi';
import { notificationQueryKeys } from '../api/queryKeys';

export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: notificationQueryKeys.unreadCount,
    queryFn: getUnreadNotificationCount,
  });
}
