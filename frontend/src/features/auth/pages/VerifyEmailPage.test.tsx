import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { VerifyEmailPage } from './VerifyEmailPage';

function renderVerification() {
  return renderWithProviders(
    <Routes>
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/" element={<p>Authenticated home</p>} />
    </Routes>,
    { initialEntries: [{ pathname: '/verify-email', state: { email: 'maya@example.com' } }] },
  );
}

describe('VerifyEmailPage', () => {
  it('verifies the code, stores credentials, and completes authentication', async () => {
    server.use(
      http.post('*/api/auth/verify-email', () =>
        HttpResponse.json({ token: 'verified-token', user: authUser }),
      ),
    );
    const { user } = renderVerification();
    expect(screen.getByLabelText('Email')).toHaveValue('maya@example.com');
    await user.type(screen.getByLabelText('Verification code'), '123456');
    await user.click(screen.getByRole('button', { name: 'Verify email' }));

    expect(await screen.findByText('Authenticated home')).toBeInTheDocument();
    expect(useAuthStore.getState()).toMatchObject({
      authStatus: 'authenticated',
      accessToken: 'verified-token',
    });
  });

  it('resends to the editable email and starts a client-only cooldown', async () => {
    let requestedEmail: unknown;
    server.use(
      http.post('*/api/auth/resend-verification', async ({ request }) => {
        requestedEmail = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderVerification();
    const resendButton = screen.getByRole('button', { name: 'Send again' });
    await user.click(resendButton);

    expect(await screen.findByText('A new verification code has been sent.')).toBeInTheDocument();
    expect(requestedEmail).toEqual({ email: 'maya@example.com' });
    expect(screen.getByRole('button', { name: /send again in 30s/i })).toBeDisabled();
  });

  it('shows a safe verification failure beside the code field', async () => {
    server.use(
      http.post('*/api/auth/verify-email', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Verification code is invalid or expired',
            path: '/api/auth/verify-email',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderVerification();
    await user.type(screen.getByLabelText('Verification code'), '123456');
    await user.click(screen.getByRole('button', { name: 'Verify email' }));

    const message = await screen.findByText('Verification code is invalid or expired');
    expect(message).toHaveAttribute('id', 'verification-code-error');
  });
});
