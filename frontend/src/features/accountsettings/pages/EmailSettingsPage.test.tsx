import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { EmailSettingsPage } from './EmailSettingsPage';

describe('EmailSettingsPage', () => {
  it('shows the email form under a Back to Settings link', () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, authUser);
    useAuthStore.getState().setAuthenticated('access-token');

    renderWithProviders(<EmailSettingsPage />, { queryClient });

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('heading', { name: 'Email' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send verification code' })).toBeInTheDocument();
  });
});
