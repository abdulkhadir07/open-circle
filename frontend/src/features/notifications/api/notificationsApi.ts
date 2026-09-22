import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseNotificationInbox, parseUnreadCount } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getNotificationInbox({ page, size }: { page: number; size: number }) {
  const response = await normalizeFailure(
    apiClient.get('/notifications', { params: { page, size } }),
  );
  return parseNotificationInbox(response.data);
}

export async function getUnreadNotificationCount() {
  const response = await normalizeFailure(apiClient.get('/notifications/unread-count'));
  return parseUnreadCount(response.data);
}

export async function markNotificationRead(notificationId: string) {
  await normalizeFailure(apiClient.patch(`/notifications/${notificationId}/read`));
}

export async function markAllNotificationsRead() {
  await normalizeFailure(apiClient.patch('/notifications/read-all'));
}
