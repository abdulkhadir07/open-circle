import { screen } from '@testing-library/react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { renderWithProviders } from '@/test/render';
import { ProtectedRoute } from './ProtectedRoute';
import { PublicOnlyRoute } from './PublicOnlyRoute';

function LoginDestination() {
  const location = useLocation();
  const state = location.state as { from?: { pathname?: string } } | null;
  return <p>Login from {state?.from?.pathname ?? 'nowhere'}</p>;
}

describe('auth route guards', () => {
  it('blocks during bootstrap and preserves the intended protected location for login', async () => {
    useAuthStore.setState({ authStatus: 'bootstrapping' });
    const view = renderWithProviders(
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/private" element={<p>Private</p>} />
        </Route>
        <Route path="/login" element={<LoginDestination />} />
      </Routes>,
      { initialEntries: ['/private'] },
    );
    expect(screen.getByText(/checking your session/i)).toBeInTheDocument();
    expect(screen.queryByText('Private')).not.toBeInTheDocument();

    useAuthStore.getState().setAnonymous();
    await screen.findByText('Login from /private');
    view.unmount();
  });

  it('keeps authenticated users out of public-only auth routes', async () => {
    useAuthStore.getState().setAuthenticated('token');
    renderWithProviders(
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<p>Login</p>} />
        </Route>
        <Route path="/" element={<p>Home</p>} />
      </Routes>,
      { initialEntries: ['/login'] },
    );
    expect(await screen.findByText('Home')).toBeInTheDocument();
    expect(screen.queryByText('Login')).not.toBeInTheDocument();
  });
});
