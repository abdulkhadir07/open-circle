import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { InfiniteData } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { notification } from '@/test/mocks/fixtures';
import type { NotificationInbox } from '../api/contracts';
import { notificationQueryKeys } from '../api/queryKeys';
import { useNotificationsRealtime } from './useNotificationsRealtime';

const { subscribeMock, onReconnectMock, playNotificationSoundMock } = vi.hoisted(() => ({
  subscribeMock:
    vi.fn<(destination: string, callback: (message: { body: string }) => void) => () => void>(),
  onReconnectMock: vi.fn<(listener: () => void) => () => void>(),
  playNotificationSoundMock: vi.fn<() => void>(),
}));

vi.mock('@/lib/realtime/stompClient', () => ({
  stompClient: {
    subscribe: subscribeMock,
    onReconnect: onReconnectMock,
  },
}));

vi.mock('../lib/playNotificationSound', () => ({
  playNotificationSound: playNotificationSoundMock,
}));

function inboxPage(notifications: (typeof notification)[]): NotificationInbox {
  return { notifications, page: 0, size: 20, totalElements: notifications.length, totalPages: 1 };
}

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const result = renderHook(() => useNotificationsRealtime(), { wrapper });
  return { queryClient, ...result };
}

describe('useNotificationsRealtime', () => {
  beforeEach(() => {
    subscribeMock.mockReset().mockReturnValue(vi.fn<() => void>());
    onReconnectMock.mockReset().mockReturnValue(vi.fn<() => void>());
    playNotificationSoundMock.mockReset();
  });

  it('subscribes to the per-user notification queue', () => {
    renderWithQueryClient();

    expect(subscribeMock).toHaveBeenCalledWith('/user/queue/notifications', expect.any(Function));
  });

  it('does nothing to an empty cache when a notification arrives', () => {
    const { queryClient } = renderWithQueryClient();
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify({ notification, unreadCount: 1 }) });

    expect(queryClient.getQueryData(notificationQueryKeys.list)).toBeUndefined();
  });

  it('upserts an incoming notification into the cached first page', () => {
    const { queryClient } = renderWithQueryClient();
    queryClient.setQueryData(notificationQueryKeys.list, {
      pages: [inboxPage([])],
      pageParams: [0],
    } satisfies InfiniteData<NotificationInbox>);
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify({ notification, unreadCount: 1 }) });

    const cached = queryClient.getQueryData<InfiniteData<NotificationInbox>>(
      notificationQueryKeys.list,
    );
    expect(cached?.pages[0]?.notifications).toEqual([notification]);
  });

  it('writes the broadcast unread count directly into the cache', () => {
    const { queryClient } = renderWithQueryClient();
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify({ notification, unreadCount: 4 }) });

    expect(queryClient.getQueryData(notificationQueryKeys.unreadCount)).toBe(4);
  });

  it('plays a sound when a notification arrives', () => {
    renderWithQueryClient();
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify({ notification, unreadCount: 1 }) });

    expect(playNotificationSoundMock).toHaveBeenCalledTimes(1);
  });

  it('does not play a sound when the incoming message fails to parse', () => {
    renderWithQueryClient();
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: 'not valid json' });

    expect(playNotificationSoundMock).not.toHaveBeenCalled();
  });

  it('invalidates the list and unread count on reconnect', () => {
    const { queryClient } = renderWithQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const reconnectListener = onReconnectMock.mock.calls[0]![0] as () => void;

    reconnectListener();

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: notificationQueryKeys.list });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: notificationQueryKeys.unreadCount });
  });

  it('unsubscribes and unlistens on unmount', () => {
    const unsubscribeQueue = vi.fn<() => void>();
    const unsubscribeReconnect = vi.fn<() => void>();
    subscribeMock.mockReturnValue(unsubscribeQueue);
    onReconnectMock.mockReturnValue(unsubscribeReconnect);

    const { unmount } = renderWithQueryClient();
    unmount();

    expect(unsubscribeQueue).toHaveBeenCalled();
    expect(unsubscribeReconnect).toHaveBeenCalled();
  });
});
