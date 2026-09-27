import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { attachmentChatMessage, chatMessage, chatRoom } from '@/test/mocks/fixtures';
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

  it('shows an attachment message with its filename, size, and caption', async () => {
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () =>
        HttpResponse.json([attachmentChatMessage]),
      ),
    );
    renderWithProviders(<MessageList roomId={chatRoom.id} currentUserId="someone-else" />);

    expect(
      await screen.findByText(attachmentChatMessage.attachment!.originalFilename),
    ).toBeInTheDocument();
    expect(screen.getByText('245 KB')).toBeInTheDocument();
    expect(screen.getByText(attachmentChatMessage.body!)).toBeInTheDocument();
  });

  it('fetches a fresh download URL and opens it when an attachment is clicked', async () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () =>
        HttpResponse.json([attachmentChatMessage]),
      ),
      http.get('*/api/attachments/:attachmentId/download-url', () =>
        HttpResponse.json({
          url: 'https://storage.example.com/flyer.png?signature=fresh',
          expiresAt: new Date().toISOString(),
        }),
      ),
    );
    const { user } = renderWithProviders(
      <MessageList roomId={chatRoom.id} currentUserId="someone-else" />,
    );

    await user.click(await screen.findByRole('button', { name: /flyer\.png/ }));

    await waitFor(() =>
      expect(openSpy).toHaveBeenCalledWith(
        'https://storage.example.com/flyer.png?signature=fresh',
        '_blank',
        'noopener,noreferrer',
      ),
    );
    openSpy.mockRestore();
  });

  it('shows an error if fetching the download URL fails', async () => {
    server.use(
      http.get('*/api/chat-rooms/:roomId/messages', () =>
        HttpResponse.json([attachmentChatMessage]),
      ),
      http.get('*/api/attachments/:attachmentId/download-url', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 403,
            error: 'FORBIDDEN',
            message: 'You must be a chat participant to perform this action',
            path: '/api/attachments/mock/download-url',
            fieldErrors: {},
          },
          { status: 403 },
        ),
      ),
    );
    const { user } = renderWithProviders(
      <MessageList roomId={chatRoom.id} currentUserId="someone-else" />,
    );

    await user.click(await screen.findByRole('button', { name: /flyer\.png/ }));

    expect(
      await screen.findByText('You must be a chat participant to perform this action'),
    ).toBeInTheDocument();
  });
});
