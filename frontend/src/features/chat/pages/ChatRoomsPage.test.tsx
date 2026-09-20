import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ChatRoomPage } from './ChatRoomPage';
import { ChatRoomsEmptyState, ChatRoomsPage } from './ChatRoomsPage';

function renderChatsShell(initialPath = '/chats', hasHiddenChatsPin = false) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/chats" element={<ChatRoomsPage />}>
        <Route index element={<ChatRoomsEmptyState />} />
        <Route path=":roomId" element={<ChatRoomPage />} />
      </Route>
    </Routes>,
    { queryClient, initialEntries: [initialPath] },
  );
}

describe('ChatRoomsPage', () => {
  it('links back to the home page', async () => {
    renderChatsShell();

    expect(screen.getByRole('link', { name: 'Back' })).toHaveAttribute('href', '/');
  });

  it('prompts to select a conversation when none is open', async () => {
    renderChatsShell();

    expect(await screen.findByText('Select a conversation to view messages.')).toBeInTheDocument();
  });

  it('shows "No hidden chats" without a PIN prompt when no PIN has been set', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderChatsShell('/chats', false);

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: 'Hidden' }));

    expect(await screen.findByText('No hidden chats.')).toBeInTheDocument();
    expect(screen.queryByText('Hidden chats are PIN-protected')).not.toBeInTheDocument();
  });

  it('shows Chats by default and switches to Hidden behind the PIN prompt', async () => {
    server.use(
      http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])),
      http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([])),
    );
    const { user } = renderChatsShell('/chats', true);

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: 'Hidden' }));

    expect(await screen.findByText('Hidden chats are PIN-protected')).toBeInTheDocument();
    expect(screen.queryByText('jordan.lee')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    expect(await screen.findByText('No hidden chats.')).toBeInTheDocument();
  });

  it('opens a conversation from the list and shows it selected', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderChatsShell();

    await user.click(await screen.findByText('jordan.lee'));

    expect(await screen.findByRole('heading', { name: 'jordan.lee' })).toBeInTheDocument();
    expect(screen.queryByText('Select a conversation to view messages.')).not.toBeInTheDocument();
  });
});
