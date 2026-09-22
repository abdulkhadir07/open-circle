import { describe, expect, it } from 'vitest';
import { notification } from '@/test/mocks/fixtures';
import type { NotificationInbox } from '../api/contracts';
import { markAllRead, markOneRead, upsertNotification } from './notificationCache';

function inboxPage(overrides: Partial<NotificationInbox> = {}): NotificationInbox {
  return {
    notifications: [notification],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    ...overrides,
  };
}

describe('upsertNotification', () => {
  it('does nothing when there are no cached pages', () => {
    expect(upsertNotification([], notification)).toEqual([]);
  });

  it('prepends a new notification onto the first page', () => {
    const older = { ...notification, id: 'older-id', occurredAt: '2026-01-01T00:00:00Z' };
    const pages = [inboxPage({ notifications: [older] })];

    const result = upsertNotification(pages, notification);

    expect(result[0]!.notifications.map((n) => n.id)).toEqual([notification.id, older.id]);
  });

  it('replaces an existing notification with the same id instead of duplicating it', () => {
    const pages = [inboxPage({ notifications: [notification] })];
    const bumped = { ...notification, occurrenceCount: 2, occurredAt: new Date().toISOString() };

    const result = upsertNotification(pages, bumped);

    expect(result[0]!.notifications).toEqual([bumped]);
  });

  it('re-sorts the first page by occurredAt descending', () => {
    const older = { ...notification, id: 'older-id', occurredAt: '2026-01-01T00:00:00Z' };
    const newest = { ...notification, id: 'newest-id', occurredAt: '2026-06-01T00:00:00Z' };
    const pages = [inboxPage({ notifications: [older, newest] })];
    const middle = { ...notification, id: 'middle-id', occurredAt: '2026-03-01T00:00:00Z' };

    const result = upsertNotification(pages, middle);

    expect(result[0]!.notifications.map((n) => n.id)).toEqual([newest.id, middle.id, older.id]);
  });

  it('leaves later pages untouched', () => {
    const secondPage = inboxPage({ notifications: [{ ...notification, id: 'second-page-id' }] });
    const pages = [inboxPage(), secondPage];

    const result = upsertNotification(pages, { ...notification, id: 'new-id' });

    expect(result[1]).toBe(secondPage);
  });
});

describe('markOneRead', () => {
  it('marks the matching notification as read across pages', () => {
    const pages = [inboxPage({ notifications: [notification] })];

    const result = markOneRead(pages, notification.id);

    expect(result[0]!.notifications[0]!.read).toBe(true);
    expect(result[0]!.notifications[0]!.readAt).not.toBeNull();
  });

  it('leaves other notifications untouched', () => {
    const other = { ...notification, id: 'other-id' };
    const pages = [inboxPage({ notifications: [notification, other] })];

    const result = markOneRead(pages, notification.id);

    expect(result[0]!.notifications[1]).toEqual(other);
  });
});

describe('markAllRead', () => {
  it('marks every notification across every page as read', () => {
    const other = { ...notification, id: 'other-id' };
    const pages = [
      inboxPage({ notifications: [notification] }),
      inboxPage({ notifications: [other] }),
    ];

    const result = markAllRead(pages);

    expect(result[0]!.notifications[0]!.read).toBe(true);
    expect(result[1]!.notifications[0]!.read).toBe(true);
  });

  it('does not overwrite the readAt of an already-read notification', () => {
    const alreadyRead = { ...notification, read: true, readAt: '2026-01-01T00:00:00Z' };
    const pages = [inboxPage({ notifications: [alreadyRead] })];

    const result = markAllRead(pages);

    expect(result[0]!.notifications[0]).toEqual(alreadyRead);
  });
});
