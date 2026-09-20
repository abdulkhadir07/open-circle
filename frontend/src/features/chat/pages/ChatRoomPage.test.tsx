import { screen, waitFor } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ChatRoomPage } from './ChatRoomPage';

function renderRoomPage(roomId: string) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/chats/:roomId" element={<ChatRoomPage />} />
      <Route path="/chats" element={<p>Chats list</p>} />
    </Routes>,
    { queryClient, initialEntries: [`/chats/${roomId}`] },
  );
}

async function openChatOptionsMenu(user: UserEvent) {
  await user.click(await screen.findByRole('button', { name: 'Chat options' }));
}

describe('ChatRoomPage', () => {
  it("shows the other participant's name and the composer, without the invite post content", async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    renderRoomPage(chatRoom.id);

    expect(await screen.findByRole('heading', { name: 'jordan.lee' })).toBeInTheDocument();
    expect(screen.queryByText(chatRoom.invitePostContent)).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Message' })).toBeEnabled();
  });

  it("still shows the other participant's name after they leave the room, and disables the composer", async () => {
    const roomAfterTheyLeft = {
      ...chatRoom,
      participants: chatRoom.participants.map((participant) =>
        participant.username === 'jordan.lee'
          ? { ...participant, active: false, left: true, leftAt: new Date().toISOString() }
          : participant,
      ),
    };
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([roomAfterTheyLeft])));
    renderRoomPage(chatRoom.id);

    expect(await screen.findByRole('heading', { name: 'jordan.lee' })).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("jordan.lee left this chat, so you can't send new messages"),
    ).toBeDisabled();
  });

  it("shows an unavailable message when the room isn't in the list", async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([])));
    renderRoomPage(chatRoom.id);

    expect(await screen.findByText("This chat isn't available anymore.")).toBeInTheDocument();
  });

  it('disables the composer and hides the chat options menu for a closed room', async () => {
    server.use(
      http.get('*/api/chat-rooms', () => HttpResponse.json([{ ...chatRoom, closed: true }])),
    );
    renderRoomPage(chatRoom.id);

    expect(await screen.findByText('This chat is closed.')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Message' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Chat options' })).not.toBeInTheDocument();
  });

  it('shows Saved and omits Save from the menu once a room is saved', async () => {
    server.use(
      http.get('*/api/chat-rooms', () => HttpResponse.json([{ ...chatRoom, saved: true }])),
    );
    const { user } = renderRoomPage(chatRoom.id);

    await screen.findByRole('heading', { name: 'jordan.lee' });
    expect(screen.getByText('Saved')).toBeInTheDocument();

    await openChatOptionsMenu(user);

    expect(screen.queryByRole('menuitem', { name: 'Save' })).not.toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Leave chat' })).toBeInTheDocument();
  });

  it('asks for confirmation before leaving, and does nothing on Cancel', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderRoomPage(chatRoom.id);
    await screen.findByRole('heading', { name: 'jordan.lee' });

    await openChatOptionsMenu(user);
    await user.click(screen.getByRole('menuitem', { name: 'Leave chat' }));

    expect(await screen.findByRole('heading', { name: 'Leave this chat?' })).toBeInTheDocument();
    expect(
      screen.getByText(/won't be able to recover it or its message history/),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('heading', { name: 'Leave this chat?' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'jordan.lee' })).toBeInTheDocument();
  });

  it('navigates back to /chats after confirming leave', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderRoomPage(chatRoom.id);
    await screen.findByRole('heading', { name: 'jordan.lee' });

    await openChatOptionsMenu(user);
    await user.click(screen.getByRole('menuitem', { name: 'Leave chat' }));
    await user.click(await screen.findByRole('button', { name: 'Leave chat' }));

    expect(await screen.findByText('Chats list')).toBeInTheDocument();
  });

  it('saves a room from the chat options menu, with no error on success', async () => {
    server.use(http.get('*/api/chat-rooms', () => HttpResponse.json([chatRoom])));
    const { user } = renderRoomPage(chatRoom.id);
    await screen.findByRole('heading', { name: 'jordan.lee' });

    await openChatOptionsMenu(user);
    await user.click(screen.getByRole('menuitem', { name: 'Save' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});
