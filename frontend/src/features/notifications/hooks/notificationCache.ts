import type { Notification, NotificationInbox } from '../api/contracts';

export function upsertNotification(
  pages: NotificationInbox[],
  incoming: Notification,
): NotificationInbox[] {
  const firstPage = pages[0];
  if (!firstPage) return pages;

  const withoutExisting = firstPage.notifications.filter(
    (notification) => notification.id !== incoming.id,
  );
  const notifications = [incoming, ...withoutExisting].sort(
    (a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime(),
  );

  return [{ ...firstPage, notifications }, ...pages.slice(1)];
}

export function markOneRead(
  pages: NotificationInbox[],
  notificationId: string,
): NotificationInbox[] {
  return pages.map((page) => ({
    ...page,
    notifications: page.notifications.map((notification) =>
      notification.id === notificationId
        ? { ...notification, read: true, readAt: notification.readAt ?? new Date().toISOString() }
        : notification,
    ),
  }));
}

export function markAllRead(pages: NotificationInbox[]): NotificationInbox[] {
  return pages.map((page) => ({
    ...page,
    notifications: page.notifications.map((notification) =>
      notification.read
        ? notification
        : { ...notification, read: true, readAt: new Date().toISOString() },
    ),
  }));
}
