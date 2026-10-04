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
import { ChatRoomsPage } from './ChatRoomsPage';

function renderChatsShell(initialPath = '/chats', hasHiddenChatsPin = false) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/chats" element={<ChatRoomsPage />} />
      <Route path="/chats/:roomId" element={<ChatRoomPage />} />
    </Routes>,
    { queryClient, initialEntries: [initialPath] },
  );
}

describe('ChatRoomsPage', () => {
  it('links the Chats and Hidden tabs', async () => {
    renderChatsShell();

    expect(screen.getByRole('link', { name: 'Chats' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Hidden' })).toHaveAttribute(
      'href',
      '/chats?tab=hidden',
    );
  });

  it('shows "No hidden chats" without a PIN prompt when no PIN has been set', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderChatsShell('/chats', false);

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Hidden' }));

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

    await user.click(screen.getByRole('link', { name: 'Hidden' }));

    expect(await screen.findByText('Hidden chats are PIN-protected')).toBeInTheDocument();
    expect(screen.queryByText('jordan.lee')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    expect(await screen.findByText('No hidden chats.')).toBeInTheDocument();
  });

  it('opens a conversation from the list as its own page', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderChatsShell();

    await user.click(await screen.findByRole('link', { name: 'Open chat with jordan.lee' }));

    expect(await screen.findByRole('heading', { name: 'jordan.lee' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Message' })).toBeEnabled();
  });
});
