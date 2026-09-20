import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { chatMessage, chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { MessageList } from './MessageList';

describe('MessageList', () => {
  it('shows an empty state when there are no messages', async () => {
    renderWithProviders(<MessageList roomId={chatRoom.id} currentUserId="someone" />);

    expect(
      await screen.findByText('No messages yet. Say hello to get the conversation started.'),
    ).toBeInTheDocument();
  });

  it("never shows the sender's name above a message, from either party", async () => {
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () => HttpResponse.json([chatMessage])),
    );
    renderWithProviders(<MessageList roomId={chatRoom.id} currentUserId="someone-else" />);

    expect(await screen.findByText(chatMessage.body!)).toBeInTheDocument();
    expect(screen.queryByText(chatMessage.senderUsername)).not.toBeInTheDocument();
  });

  it('shows "You left" for a participant-left message from the current user', async () => {
    const leftMessage = { ...chatMessage, type: 'PARTICIPANT_LEFT' as const, body: null };
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () => HttpResponse.json([leftMessage])),
    );
    renderWithProviders(<MessageList roomId={chatRoom.id} currentUserId={leftMessage.senderId} />);

    expect(await screen.findByText('You left')).toBeInTheDocument();
  });

  it('shows "{username} left the chat" for a participant-left message from someone else', async () => {
    const leftMessage = { ...chatMessage, type: 'PARTICIPANT_LEFT' as const, body: null };
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () => HttpResponse.json([leftMessage])),
    );
    renderWithProviders(<MessageList roomId={chatRoom.id} currentUserId="someone-else" />);

    expect(await screen.findByText('jordan.lee left the chat')).toBeInTheDocument();
  });
});
