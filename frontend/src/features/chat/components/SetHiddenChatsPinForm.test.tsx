import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { SetHiddenChatsPinForm } from './SetHiddenChatsPinForm';

describe('SetHiddenChatsPinForm', () => {
  it('saves the PIN and reports success', async () => {
    const onSuccess = vi.fn<(pin: string) => void>();
    const { user } = renderWithProviders(
      <SetHiddenChatsPinForm onSuccess={onSuccess} onCancel={vi.fn<() => void>()} />,
    );

    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Save PIN' }));

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledWith('4321'));
  });

  it('rejects mismatched PINs without calling the API', async () => {
    const onSuccess = vi.fn<(pin: string) => void>();
    const { user } = renderWithProviders(
      <SetHiddenChatsPinForm onSuccess={onSuccess} onCancel={vi.fn<() => void>()} />,
    );

    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Save PIN' }));

    expect(await screen.findByText('PINs do not match.')).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('shows a server error message on failure', async () => {
    server.use(
      http.put('*/api/users/me/hidden-chats-pin', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Something went wrong. Please try again.',
            path: '/api/users/me/hidden-chats-pin',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );
    const { user } = renderWithProviders(
      <SetHiddenChatsPinForm
        onSuccess={vi.fn<(pin: string) => void>()}
        onCancel={vi.fn<() => void>()}
      />,
    );

    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Save PIN' }));

    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
  });

  it('calls onCancel when Cancel is clicked', async () => {
    const onCancel = vi.fn<() => void>();
    const { user } = renderWithProviders(
      <SetHiddenChatsPinForm onSuccess={vi.fn<(pin: string) => void>()} onCancel={onCancel} />,
    );

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalled();
  });
});
