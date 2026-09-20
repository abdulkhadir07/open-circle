import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { MessageComposer } from './MessageComposer';

describe('MessageComposer', () => {
  it('sends a message and clears the input, with no error on success', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'Hey, running 5 minutes late!');
    await user.click(screen.getByRole('button', { name: 'Send message' }));

    await waitFor(() => expect(input).toHaveValue(''));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('sends on Enter without needing the button', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'On my way{Enter}');

    await waitFor(() => expect(input).toHaveValue(''));
  });

  it('does not send on Shift+Enter, allowing a newline instead', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'Line one{Shift>}{Enter}{/Shift}Line two');

    expect(input).toHaveValue('Line one\nLine two');
  });

  it('disables the composer when the room is closed', () => {
    renderWithProviders(<MessageComposer roomId={chatRoom.id} disabled />);

    expect(screen.getByRole('textbox', { name: 'Message' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send message' })).toBeDisabled();
  });

  it('shows an error if sending fails', async () => {
    server.use(
      http.post('*/api/chat-rooms/:roomId/messages', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 403,
            error: 'FORBIDDEN',
            message: 'You are not an active participant in this chat',
            path: '/api/chat-rooms/mock/messages',
            fieldErrors: {},
          },
          { status: 403 },
        ),
      ),
    );
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    await user.type(screen.getByRole('textbox', { name: 'Message' }), 'Hello?{Enter}');

    expect(
      await screen.findByText('You are not an active participant in this chat'),
    ).toBeInTheDocument();
  });
});
