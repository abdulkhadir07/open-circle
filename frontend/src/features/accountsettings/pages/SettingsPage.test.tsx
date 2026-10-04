import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { accountSessions, authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { SettingsPage } from './SettingsPage';

function renderSettingsPage(hasHiddenChatsPin = false) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, hasHiddenChatsPin });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<SettingsPage />, { queryClient });
}

describe('SettingsPage', () => {
  it('links to each settings section', () => {
    renderSettingsPage();

    expect(screen.getByRole('link', { name: /Personal info/ })).toHaveAttribute(
      'href',
      '/settings/personal-info',
    );
    expect(screen.getByRole('link', { name: /Password/ })).toHaveAttribute(
      'href',
      '/settings/password',
    );
    expect(screen.getByRole('link', { name: /Email/ })).toHaveAttribute('href', '/settings/email');
    expect(screen.getByRole('link', { name: /Hidden chats PIN/ })).toHaveAttribute(
      'href',
      '/settings/pin',
    );
    expect(screen.getByRole('link', { name: /Sessions/ })).toHaveAttribute(
      'href',
      '/settings/sessions',
    );
  });

  it("shows the account's current email as the Email row subtitle", () => {
    renderSettingsPage();

    expect(screen.getByText(authUser.email)).toBeInTheDocument();
  });

  it('shows a profile summary card with a link to the profile, without the email in it', () => {
    renderSettingsPage();

    expect(screen.getByText(`${authUser.firstName} ${authUser.lastName}`)).toBeInTheDocument();
    expect(screen.getByText(`@${authUser.username}`)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View profile' })).toHaveAttribute(
      'href',
      `/profile/${authUser.id}`,
    );
  });

  it('groups the rows under Account, Security, and Appearance', () => {
    renderSettingsPage();

    for (const heading of ['Account', 'Security', 'Appearance']) {
      expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument();
    }
  });

  it('links the Theme row to the appearance page and shows the current choice', () => {
    renderSettingsPage();

    expect(screen.getByRole('link', { name: /Theme/ })).toHaveAttribute(
      'href',
      '/settings/appearance',
    );
    expect(screen.getByText('System')).toBeInTheDocument();
  });

  it('invites setting up a PIN when none exists yet', () => {
    renderSettingsPage(false);

    expect(screen.getByText('No PIN set yet')).toBeInTheDocument();
  });

  it('shows the PIN is already set once one exists', () => {
    renderSettingsPage(true);

    expect(screen.getByText('PIN set — change it here')).toBeInTheDocument();
  });

  it('shows the active device count as the Sessions row subtitle', async () => {
    renderSettingsPage();

    expect(
      await screen.findByText(`${accountSessions.length} devices signed in`),
    ).toBeInTheDocument();
  });

  it('signs out and navigates to login when Sign out is clicked', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, authUser);
    useAuthStore.getState().setAuthenticated('access-token');
    const { user } = renderWithProviders(
      <Routes>
        <Route path="/" element={<SettingsPage />} />
        <Route path="/login" element={<p>Signed out</p>} />
      </Routes>,
      { queryClient },
    );

    await user.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(await screen.findByText('Signed out')).toBeInTheDocument();
    expect(useAuthStore.getState().authStatus).toBe('anonymous');
    expect(queryClient.getQueryData(authQueryKeys.currentUser)).toBeUndefined();
  });
});
