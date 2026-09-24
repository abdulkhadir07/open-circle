import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { SessionsSettingsPage } from './SessionsSettingsPage';

function renderPage() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route element={<ProtectedRoute />}>
        <Route path="/settings/sessions" element={<SessionsSettingsPage />} />
      </Route>
      <Route path="/login" element={<p>Login page</p>} />
    </Routes>,
    { queryClient, initialEntries: ['/settings/sessions'] },
  );
}

describe('SessionsSettingsPage', () => {
  it('shows the sessions list under a Back to Settings link', async () => {
    renderPage();

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('heading', { name: 'Sessions' })).toBeInTheDocument();
    expect(await screen.findByText('This device')).toBeInTheDocument();
  });

  it('redirects to login after signing out everywhere', async () => {
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Sign out everywhere' }));

    expect(await screen.findByText('Login page')).toBeInTheDocument();
  });
});
