import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HiddenChatsPinForm } from './HiddenChatsPinForm';

function renderForm(hasHiddenChatsPin: boolean) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<HiddenChatsPinForm />, { queryClient });
}

function renderFormWithRouter(hasHiddenChatsPin: boolean) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/settings/pin" element={<HiddenChatsPinForm />} />
      <Route path="/settings" element={<p>Settings page</p>} />
    </Routes>,
    { queryClient, initialEntries: ['/settings/pin'] },
  );
}

describe('HiddenChatsPinForm', () => {
  it('offers to set a PIN with no password field when none is set', async () => {
    renderForm(false);

    expect(await screen.findByLabelText('PIN')).toBeInTheDocument();
    expect(screen.queryByLabelText('Current password')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Set PIN' })).toBeInTheDocument();
  });

  it('requires the current password to change an existing PIN', async () => {
    renderForm(true);

    expect(await screen.findByLabelText('Current password')).toBeInTheDocument();
    expect(screen.getByLabelText('New PIN')).toBeInTheDocument();
    const submit = screen.getByRole('button', { name: 'Change PIN' });
    expect(submit).toBeDisabled();
  });

  it('rejects mismatched PINs without calling the API', async () => {
    const { user } = renderForm(false);

    await user.type(await screen.findByLabelText('PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '1234');
    await user.click(screen.getByRole('button', { name: 'Set PIN' }));

    expect(await screen.findByText('PINs do not match.')).toBeInTheDocument();
  });

  it('replaces the form with a "PIN set" success panel on first setup', async () => {
    const { user } = renderForm(false);

    await user.type(await screen.findByLabelText('PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Set PIN' }));

    expect(await screen.findByText('PIN set')).toBeInTheDocument();
    expect(screen.queryByLabelText('PIN')).not.toBeInTheDocument();
  });

  it('returns to Settings when Close is clicked on the success panel', async () => {
    const { user } = renderFormWithRouter(false);

    await user.type(await screen.findByLabelText('PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Set PIN' }));
    await screen.findByText('PIN set');

    await user.click(screen.getByRole('button', { name: 'Close' }));

    expect(await screen.findByText('Settings page')).toBeInTheDocument();
  });

  it('shows a "PIN changed" success panel when updating an existing PIN', async () => {
    const { user } = renderForm(true);

    await user.type(await screen.findByLabelText('Current password'), 'currentpass');
    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm new PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Change PIN' }));

    expect(await screen.findByText('PIN changed')).toBeInTheDocument();
  });

  it('shows a backend error on failure', async () => {
    server.use(
      http.put('*/api/users/me/hidden-chats-pin', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Current password is incorrect',
            path: '/api/users/me/hidden-chats-pin',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderForm(true);

    await user.type(await screen.findByLabelText('Current password'), 'wrongpass');
    await user.type(screen.getByLabelText('New PIN'), '4321');
    await user.type(screen.getByLabelText('Confirm new PIN'), '4321');
    await user.click(screen.getByRole('button', { name: 'Change PIN' }));

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
  });
});
