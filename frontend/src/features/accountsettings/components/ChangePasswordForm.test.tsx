import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { ChangePasswordForm } from './ChangePasswordForm';

describe('ChangePasswordForm', () => {
  it('validates the new password before submitting', async () => {
    const { user } = renderWithProviders(<ChangePasswordForm />);

    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.type(screen.getByLabelText('New password'), 'short');
    await user.type(screen.getByLabelText('Confirm new password'), 'short');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument();
  });

  it('shows a mismatch error when the confirmation differs', async () => {
    const { user } = renderWithProviders(<ChangePasswordForm />);

    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword2');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument();
  });

  it('replaces the form with a success panel once the password changes', async () => {
    const { user } = renderWithProviders(<ChangePasswordForm />);

    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('Password changed')).toBeInTheDocument();
    expect(screen.queryByLabelText('Current password')).not.toBeInTheDocument();
  });

  it('returns to Settings when Close is clicked on the success panel', async () => {
    const { user } = renderWithProviders(
      <Routes>
        <Route path="/settings/password" element={<ChangePasswordForm />} />
        <Route path="/settings" element={<p>Settings page</p>} />
      </Routes>,
      { initialEntries: ['/settings/password'] },
    );

    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: 'Change password' }));
    await screen.findByText('Password changed');

    await user.click(screen.getByRole('button', { name: 'Close' }));

    expect(await screen.findByText('Settings page')).toBeInTheDocument();
  });

  it('shows the backend error when the current password is wrong', async () => {
    server.use(
      http.put('*/api/users/me/password', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Current password is incorrect',
            path: '/api/users/me/password',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderWithProviders(<ChangePasswordForm />);

    await user.type(screen.getByLabelText('Current password'), 'wrongpass');
    await user.type(screen.getByLabelText('New password'), 'newpassword1');
    await user.type(screen.getByLabelText('Confirm new password'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
    expect(screen.queryByText('Password changed')).not.toBeInTheDocument();
  });
});
