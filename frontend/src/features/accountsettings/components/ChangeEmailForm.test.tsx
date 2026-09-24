import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ChangeEmailForm } from './ChangeEmailForm';

function renderForm() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<ChangeEmailForm />, { queryClient });
}

describe('ChangeEmailForm', () => {
  it("shows the user's current email", async () => {
    renderForm();

    expect(await screen.findByText(authUser.email)).toBeInTheDocument();
  });

  it('moves to the code step after successfully requesting a change', async () => {
    const { user } = renderForm();

    await user.type(screen.getByLabelText('New email'), 'new@example.com');
    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.click(screen.getByRole('button', { name: 'Send verification code' }));

    expect(await screen.findByText(/Enter the 6-digit code we sent to/)).toBeInTheDocument();
    expect(screen.getByText('new@example.com')).toBeInTheDocument();
  });

  it('shows a backend error on the request step without advancing', async () => {
    server.use(
      http.post('*/api/users/me/email-change', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 409,
            error: 'CONFLICT',
            message: 'Email is already registered',
            path: '/api/users/me/email-change',
            fieldErrors: {},
          },
          { status: 409 },
        ),
      ),
    );
    const { user } = renderForm();

    await user.type(screen.getByLabelText('New email'), 'taken@example.com');
    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.click(screen.getByRole('button', { name: 'Send verification code' }));

    expect(await screen.findByText('Email is already registered')).toBeInTheDocument();
    expect(screen.queryByText(/Enter the 6-digit code we sent to/)).not.toBeInTheDocument();
  });

  it('shows an invalid-code error on the verify step', async () => {
    server.use(
      http.post('*/api/users/me/email-change/verify', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Invalid or expired email change code',
            path: '/api/users/me/email-change/verify',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderForm();

    await user.type(screen.getByLabelText('New email'), 'new@example.com');
    await user.type(screen.getByLabelText('Current password'), 'currentpass');
    await user.click(screen.getByRole('button', { name: 'Send verification code' }));
    await screen.findByText(/Enter the 6-digit code we sent to/);

    await user.type(screen.getByRole('textbox', { name: 'Verification code' }), '123456');
    await user.click(screen.getByRole('button', { name: 'Verify email' }));

    expect(await screen.findByText('Invalid or expired email change code')).toBeInTheDocument();
  });
});
