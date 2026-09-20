import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HiddenChatRoomList } from './HiddenChatRoomList';

function renderAsAuthUser() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<HiddenChatRoomList pin="1234" />, { queryClient });
}

describe('HiddenChatRoomList', () => {
  it('shows an empty state when there are no hidden chats', async () => {
    renderAsAuthUser();

    expect(await screen.findByText('No hidden chats.')).toBeInTheDocument();
  });

  it('lists a hidden room with an Unhide action', async () => {
    server.use(http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([chatRoom])));
    renderAsAuthUser();

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Unhide' })).toBeInTheDocument();
  });

  it('unhides a room when Unhide is clicked, with no error on success', async () => {
    server.use(http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([chatRoom])));
    const { user } = renderAsAuthUser();

    await user.click(await screen.findByRole('button', { name: 'Unhide' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});
