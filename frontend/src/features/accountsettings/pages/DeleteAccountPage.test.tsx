import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { DeleteAccountPage } from './DeleteAccountPage';

function renderPage() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  const result = renderWithProviders(
    <Routes>
      <Route path="/settings/delete-account" element={<DeleteAccountPage />} />
      <Route path="/login" element={<p>Deletion complete</p>} />
    </Routes>,
    { queryClient, initialEntries: ['/settings/delete-account'] },
  );
  return { ...result, queryClient };
}

describe('DeleteAccountPage', () => {
  it('explains that deletion is immediate, permanent, and removes active account data', () => {
    renderPage();

    expect(screen.getByText('This cannot be undone')).toBeInTheDocument();
    expect(
      screen.getByText(/permanently delete all account information stored about you/i),
    ).toBeVisible();
    expect(screen.getByText(/will not be retained in the active service/i)).toBeVisible();
    expect(screen.getByText(/conversations connected to invites you created/i)).toBeVisible();
  });

  it('requires the current password and exact DELETE confirmation', async () => {
    const { user } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Permanently delete account' }));
    expect(await screen.findByText('Current password is required')).toBeVisible();

    await user.type(screen.getByLabelText('Current password'), 'Password123!');
    await user.type(screen.getByLabelText('Type DELETE to confirm'), 'delete');
    await user.click(screen.getByRole('button', { name: 'Permanently delete account' }));
    expect(await screen.findByText('Type DELETE to confirm account deletion')).toBeVisible();
  });

  it('deletes the account, clears auth state, and returns to login', async () => {
    let requestBody: unknown;
    server.use(
      http.delete('*/api/users/me', async ({ request }) => {
        requestBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user, queryClient } = renderPage();

    await user.type(screen.getByLabelText('Current password'), 'Password123!');
    await user.type(screen.getByLabelText('Type DELETE to confirm'), 'DELETE');
    await user.click(screen.getByRole('button', { name: 'Permanently delete account' }));

    expect(await screen.findByText('Deletion complete')).toBeVisible();
    expect(requestBody).toEqual({ currentPassword: 'Password123!', confirmation: 'DELETE' });
    expect(useAuthStore.getState().authStatus).toBe('anonymous');
    expect(queryClient.getQueryData(authQueryKeys.currentUser)).toBeUndefined();
  });

  it('keeps the account signed in when the server rejects deletion', async () => {
    server.use(
      http.delete('*/api/users/me', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Current password is incorrect',
            path: '/api/users/me',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderPage();

    await user.type(screen.getByLabelText('Current password'), 'WrongPassword!');
    await user.type(screen.getByLabelText('Type DELETE to confirm'), 'DELETE');
    await user.click(screen.getByRole('button', { name: 'Permanently delete account' }));

    expect(await screen.findByText('Current password is incorrect')).toBeVisible();
    expect(useAuthStore.getState().authStatus).toBe('authenticated');
  });
});
