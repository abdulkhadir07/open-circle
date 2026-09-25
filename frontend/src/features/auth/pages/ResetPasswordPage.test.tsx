import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { ResetPasswordPage } from './ResetPasswordPage';

function NewPasswordDestination() {
  const location = useLocation();
  const state = location.state as { email?: string; code?: string } | null;
  return (
    <p>
      New password step for {state?.email} with code {state?.code}
    </p>
  );
}

function renderResetPassword() {
  return renderWithProviders(
    <Routes>
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/reset-password/new-password" element={<NewPasswordDestination />} />
    </Routes>,
    { initialEntries: [{ pathname: '/reset-password', state: { email: 'maya@example.com' } }] },
  );
}

describe('ResetPasswordPage', () => {
  it('pre-fills the email from route state, editable', () => {
    renderResetPassword();

    expect(screen.getByLabelText('Email')).toHaveValue('maya@example.com');
  });

  it('moves to the new-password step carrying the email and code, without calling reset-password', async () => {
    let resetCalled = false;
    server.use(
      http.post('*/api/auth/reset-password', () => {
        resetCalled = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderResetPassword();

    await user.type(screen.getByLabelText('Reset code'), '123456');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(
      await screen.findByText('New password step for maya@example.com with code 123456'),
    ).toBeInTheDocument();
    expect(resetCalled).toBe(false);
  });

  it('rejects an incomplete code before continuing', async () => {
    const { user } = renderResetPassword();

    await user.type(screen.getByLabelText('Reset code'), '123');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('Enter the 6-digit reset code')).toBeInTheDocument();
  });

  it('resends the code to the editable email and starts a cooldown', async () => {
    let requestedEmail: unknown;
    server.use(
      http.post('*/api/auth/forgot-password', async ({ request }) => {
        requestedEmail = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderResetPassword();

    await user.click(screen.getByRole('button', { name: 'Send again' }));

    expect(await screen.findByText('A new reset code has been sent.')).toBeInTheDocument();
    expect(requestedEmail).toEqual({ email: 'maya@example.com' });
    expect(screen.getByRole('button', { name: /send again in 30s/i })).toBeDisabled();
  });
});
