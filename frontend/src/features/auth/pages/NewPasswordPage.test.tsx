import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { NewPasswordPage } from './NewPasswordPage';

function renderNewPassword(
  state: { email?: string; code?: string } | null = {
    email: 'maya@example.com',
    code: '123456',
  },
) {
  return renderWithProviders(
    <Routes>
      <Route path="/reset-password/new-password" element={<NewPasswordPage />} />
      <Route path="/reset-password" element={<p>Reset password code step</p>} />
      <Route path="/login" element={<p>Login page</p>} />
    </Routes>,
    { initialEntries: [{ pathname: '/reset-password/new-password', state }] },
  );
}

describe('NewPasswordPage', () => {
  it('redirects back to the code step when arrived at directly, without a code', () => {
    renderNewPassword(null);

    expect(screen.getByText('Reset password code step')).toBeInTheDocument();
  });

  it('resets the password and navigates to login with a confirmation flag', async () => {
    let requestedBody: unknown;
    server.use(
      http.post('*/api/auth/reset-password', async ({ request }) => {
        requestedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderNewPassword();

    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(await screen.findByText('Login page')).toBeInTheDocument();
    expect(requestedBody).toEqual({
      email: 'maya@example.com',
      code: '123456',
      newPassword: 'newpassword1',
    });
  });

  it('rejects a mismatched confirmation without calling the API', async () => {
    const { user } = renderNewPassword();

    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'different1');
    await user.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument();
  });

  it('shows an invalid-code error and offers a way back to the code step', async () => {
    server.use(
      http.post('*/api/auth/reset-password', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Invalid or expired password reset code',
            path: '/api/auth/reset-password',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderNewPassword();

    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(await screen.findByText(/Invalid or expired password reset code/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go back' })).toHaveAttribute(
      'href',
      '/reset-password',
    );
  });
});
