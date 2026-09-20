import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HiddenChatsPinGate } from './HiddenChatsPinGate';

function renderAsAuthUser(hasHiddenChatsPin: boolean) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<HiddenChatsPinGate />, { queryClient });
}

describe('HiddenChatsPinGate', () => {
  it('shows "No hidden chats" directly when no PIN has ever been set', async () => {
    renderAsAuthUser(false);

    expect(await screen.findByText('No hidden chats.')).toBeInTheDocument();
  });

  it('shows the PIN prompt when a PIN has been set', async () => {
    renderAsAuthUser(true);

    expect(await screen.findByText('Hidden chats are PIN-protected')).toBeInTheDocument();
  });

  it('reveals the hidden chat list once the correct PIN is entered', async () => {
    server.use(http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([chatRoom])));
    const { user } = renderAsAuthUser(true);

    await user.type(await screen.findByLabelText('PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();
  });
});
