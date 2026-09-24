import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { SessionsList } from './SessionsList';

describe('SessionsList', () => {
  it('lists the active sessions, marking the current one', async () => {
    renderWithProviders(<SessionsList />);

    expect(await screen.findByText('This device')).toBeInTheDocument();
    expect(screen.getByText(/Chrome on macOS/)).toBeInTheDocument();
    expect(screen.getByText(/Safari on iOS/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Revoke' })).toBeInTheDocument();
  });

  it('revokes a non-current session without signing the user out', async () => {
    let revoked = false;
    server.use(
      http.delete('*/api/users/me/sessions/:sessionId', () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    useAuthStore.getState().setAuthenticated('access-token');
    const { user } = renderWithProviders(<SessionsList />);

    await user.click(await screen.findByRole('button', { name: 'Revoke' }));

    await waitFor(() => expect(revoked).toBe(true));
    expect(useAuthStore.getState().authStatus).toBe('authenticated');
  });

  it('signs the user out when "Sign out everywhere" is used', async () => {
    useAuthStore.getState().setAuthenticated('access-token');
    const { user } = renderWithProviders(<SessionsList />);

    await user.click(await screen.findByRole('button', { name: 'Sign out everywhere' }));

    await waitFor(() => expect(useAuthStore.getState().authStatus).toBe('anonymous'));
  });
});
