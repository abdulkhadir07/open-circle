import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ChatRoomList } from './ChatRoomList';

function renderAsAuthUser(hasHiddenChatsPin = true) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<ChatRoomList />, { queryClient });
}

describe('ChatRoomList', () => {
  it('shows an empty state when there are no chats', async () => {
    renderAsAuthUser();

    expect(
      await screen.findByText(
        'No conversations yet. Accepting or being accepted into an invite starts one.',
      ),
    ).toBeInTheDocument();
  });

  it('lists a room by the other participant, excluding the current user', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    renderAsAuthUser();

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();
    expect(screen.queryByText(authUser.username)).not.toBeInTheDocument();
    expect(screen.queryByText(chatRoom.invitePostContent)).not.toBeInTheDocument();
    expect(screen.queryByText('Closed')).not.toBeInTheDocument();
  });

  it('still lists a room by the other participant after they leave', async () => {
    const roomAfterTheyLeft = {
      ...chatRoom,
      participants: chatRoom.participants.map((participant) =>
        participant.username === 'jordan.lee'
          ? { ...participant, active: false, left: true, leftAt: new Date().toISOString() }
          : participant,
      ),
    };
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([roomAfterTheyLeft])));
    renderAsAuthUser();

    expect(await screen.findByText('jordan.lee')).toBeInTheDocument();
  });

  it('shows Closed, with no Hide action, for a closed room', async () => {
    server.use(
      http.get('*/api/chat-rooms', () => HttpResponse.json([{ ...chatRoom, closed: true }])),
    );
    renderAsAuthUser();

    expect(await screen.findByText('Closed')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hide' })).not.toBeInTheDocument();
  });

  it('hides a room directly when Hide is clicked and a PIN is already set', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderAsAuthUser(true);

    await user.click(await screen.findByRole('button', { name: 'Hide' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(screen.queryByText('Set up a Hidden chats PIN')).not.toBeInTheDocument();
  });

  it('requires setting up a PIN before the first hide, then hides the room', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderAsAuthUser(false);

    await user.click(await screen.findByRole('button', { name: 'Hide' }));

    expect(await screen.findByText('Set up a Hidden chats PIN')).toBeInTheDocument();

    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Save PIN' }));

    await waitFor(() =>
      expect(screen.queryByText('Set up a Hidden chats PIN')).not.toBeInTheDocument(),
    );
  });

  it('cancelling PIN setup does not hide the room', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderAsAuthUser(false);

    await user.click(await screen.findByRole('button', { name: 'Hide' }));
    expect(await screen.findByText('Set up a Hidden chats PIN')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByText('Set up a Hidden chats PIN')).not.toBeInTheDocument();
    expect(screen.getByText('jordan.lee')).toBeInTheDocument();
  });

  it("links to the other participant's profile for a 1:1 room", async () => {
    const jordan = chatRoom.participants.find((p) => p.username === 'jordan.lee');
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    renderAsAuthUser();

    await screen.findByText('jordan.lee');

    expect(screen.getByRole('link', { name: "View jordan.lee's profile" })).toHaveAttribute(
      'href',
      `/profile/${jordan?.userId}`,
    );
  });
});
