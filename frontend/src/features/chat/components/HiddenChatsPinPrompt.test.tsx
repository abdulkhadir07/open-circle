import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { HiddenChatsPinPrompt } from './HiddenChatsPinPrompt';

function apiError(status: number, message: string) {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      error: 'ERROR',
      message,
      path: '/api/chat-rooms/hidden',
      fieldErrors: {},
    },
    { status },
  );
}

describe('HiddenChatsPinPrompt', () => {
  it('unlocks when the PIN is correct', async () => {
    server.use(http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([chatRoom])));
    const onUnlock = vi.fn<(pin: string) => void>();
    const { user } = renderWithProviders(<HiddenChatsPinPrompt onUnlock={onUnlock} />);

    await user.type(screen.getByLabelText('PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    await vi.waitFor(() => expect(onUnlock).toHaveBeenCalledWith('1234'));
  });

  it('shows an error message for an incorrect PIN', async () => {
    server.use(http.get('*/api/chat-rooms/hidden', () => apiError(401, 'Incorrect PIN')));
    const { user } = renderWithProviders(
      <HiddenChatsPinPrompt onUnlock={vi.fn<(pin: string) => void>()} />,
    );

    await user.type(screen.getByLabelText('PIN'), '0000');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    expect(await screen.findByText('Incorrect PIN')).toBeInTheDocument();
  });

  it('shows an error message when locked out', async () => {
    server.use(
      http.get('*/api/chat-rooms/hidden', () =>
        apiError(429, 'Too many incorrect attempts. Try again later'),
      ),
    );
    const { user } = renderWithProviders(
      <HiddenChatsPinPrompt onUnlock={vi.fn<(pin: string) => void>()} />,
    );

    await user.type(screen.getByLabelText('PIN'), '0000');
    await user.click(screen.getByRole('button', { name: 'Unlock' }));

    expect(
      await screen.findByText('Too many incorrect attempts. Try again later'),
    ).toBeInTheDocument();
  });
});
