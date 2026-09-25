import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { ForgotPasswordPage } from './ForgotPasswordPage';

function renderForgotPassword() {
  return renderWithProviders(
    <Routes>
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<p>Reset password page</p>} />
    </Routes>,
    { initialEntries: ['/forgot-password'] },
  );
}

describe('ForgotPasswordPage', () => {
  it('requests a reset code and moves to the code-entry step for a real account', async () => {
    let requestedEmail: unknown;
    server.use(
      http.post('*/api/auth/forgot-password', async ({ request }) => {
        requestedEmail = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderForgotPassword();

    await user.type(screen.getByLabelText('Email'), 'maya@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset code' }));

    expect(await screen.findByText('Reset password page')).toBeInTheDocument();
    expect(requestedEmail).toEqual({ email: 'maya@example.com' });
  });

  it('moves to the code-entry step even for an email with no account, without revealing that', async () => {
    server.use(
      http.post('*/api/auth/forgot-password', () => new HttpResponse(null, { status: 204 })),
    );
    const { user } = renderForgotPassword();

    await user.type(screen.getByLabelText('Email'), 'nobody@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset code' }));

    expect(await screen.findByText('Reset password page')).toBeInTheDocument();
  });

  it('shows an error and stays on the page when the request itself fails', async () => {
    server.use(
      http.post('*/api/auth/forgot-password', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Something went wrong. Please try again.',
            path: '/api/auth/forgot-password',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );
    const { user } = renderForgotPassword();

    await user.type(screen.getByLabelText('Email'), 'maya@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset code' }));

    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    expect(screen.queryByText('Reset password page')).not.toBeInTheDocument();
  });
});
