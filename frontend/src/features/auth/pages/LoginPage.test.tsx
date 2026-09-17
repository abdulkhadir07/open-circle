import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { LoginPage } from './LoginPage';

function VerifyDestination() {
  const location = useLocation();
  const state = location.state as { email?: string } | null;
  return <p>Verify {state?.email}</p>;
}

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/verify-email" element={<VerifyDestination />} />
      <Route path="/planned" element={<p>Planned destination</p>} />
    </Routes>,
    {
      initialEntries: [
        { pathname: '/login', state: { from: { pathname: '/planned', search: '', hash: '' } } },
      ],
    },
  );
}

async function enterCredentials(user: ReturnType<typeof renderWithProviders>['user']) {
  await user.type(screen.getByLabelText('Email'), 'maya@example.com');
  await user.type(screen.getByLabelText('Password'), 'open-circle-strong');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
}

function loginFailure(status: number, message: string) {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      error: status === 401 ? 'UNAUTHORIZED' : 'FORBIDDEN',
      message,
      path: '/api/auth/login',
      fieldErrors: {},
    },
    { status },
  );
}

describe('LoginPage', () => {
  it('authenticates, seeds the user, and returns to the intended route', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json({ token: 'login-token', user: authUser }),
      ),
    );
    const { user } = renderLogin();
    await enterCredentials(user);

    expect(await screen.findByText('Planned destination')).toBeInTheDocument();
    expect(useAuthStore.getState()).toMatchObject({
      authStatus: 'authenticated',
      accessToken: 'login-token',
    });
  });

  it('shows invalid credentials without attempting a refresh', async () => {
    let refreshCalls = 0;
    server.use(
      http.post('*/api/auth/login', () => loginFailure(401, 'Invalid email or password')),
      http.post('*/api/auth/refresh', () => {
        refreshCalls += 1;
        return HttpResponse.json({ token: 'unexpected' });
      }),
    );
    const { user } = renderLogin();
    await enterCredentials(user);

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
    expect(refreshCalls).toBe(0);
  });

  it('redirects an unverified account to email verification', async () => {
    server.use(
      http.post('*/api/auth/login', () => loginFailure(403, 'Email verification is required')),
    );
    const { user } = renderLogin();
    await enterCredentials(user);

    expect(await screen.findByText('Verify maya@example.com')).toBeInTheDocument();
  });
});
