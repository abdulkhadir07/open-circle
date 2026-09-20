import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { chatMessage } from '@/test/mocks/fixtures';
import { chatQueryKeys } from '../api/queryKeys';
import { useChatRoomRealtime } from './useChatRoomRealtime';

const { subscribeMock, onReconnectMock } = vi.hoisted(() => ({
  subscribeMock:
    vi.fn<(destination: string, callback: (message: { body: string }) => void) => () => void>(),
  onReconnectMock: vi.fn<(listener: () => void) => () => void>(),
}));

vi.mock('@/lib/realtime/stompClient', () => ({
  stompClient: {
    subscribe: subscribeMock,
    onReconnect: onReconnectMock,
  },
}));

function renderWithQueryClient(roomId: string | undefined) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  const result = renderHook(() => useChatRoomRealtime(roomId), { wrapper });
  return { queryClient, ...result };
}

describe('useChatRoomRealtime', () => {
  beforeEach(() => {
    subscribeMock.mockReset().mockReturnValue(vi.fn<() => void>());
    onReconnectMock.mockReset().mockReturnValue(vi.fn<() => void>());
  });

  it('subscribes to the room topic for a defined roomId', () => {
    renderWithQueryClient(chatMessage.roomId);

    expect(subscribeMock).toHaveBeenCalledWith(
      `/topic/chat-rooms/${chatMessage.roomId}`,
      expect.any(Function),
    );
  });

  it('does nothing when roomId is undefined', () => {
    renderWithQueryClient(undefined);

    expect(subscribeMock).not.toHaveBeenCalled();
  });

  it('appends an incoming message into the query cache', () => {
    const { queryClient } = renderWithQueryClient(chatMessage.roomId);
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify(chatMessage) });

    expect(queryClient.getQueryData(chatQueryKeys.messages(chatMessage.roomId))).toEqual([
      chatMessage,
    ]);
  });

  it('does not duplicate a message delivered twice', () => {
    const { queryClient } = renderWithQueryClient(chatMessage.roomId);
    const deliver = subscribeMock.mock.calls[0]![1] as (message: { body: string }) => void;

    deliver({ body: JSON.stringify(chatMessage) });
    deliver({ body: JSON.stringify(chatMessage) });

    expect(queryClient.getQueryData(chatQueryKeys.messages(chatMessage.roomId))).toEqual([
      chatMessage,
    ]);
  });

  it('invalidates the message list on reconnect', () => {
    const { queryClient } = renderWithQueryClient(chatMessage.roomId);
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    const reconnectListener = onReconnectMock.mock.calls[0]![0] as () => void;

    reconnectListener();

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: chatQueryKeys.messages(chatMessage.roomId),
    });
  });

  it('unsubscribes and unlistens on unmount', () => {
    const unsubscribeTopic = vi.fn<() => void>();
    const unsubscribeReconnect = vi.fn<() => void>();
    subscribeMock.mockReturnValue(unsubscribeTopic);
    onReconnectMock.mockReturnValue(unsubscribeReconnect);

    const { unmount } = renderWithQueryClient(chatMessage.roomId);
    unmount();

    expect(unsubscribeTopic).toHaveBeenCalled();
    expect(unsubscribeReconnect).toHaveBeenCalled();
  });
});
