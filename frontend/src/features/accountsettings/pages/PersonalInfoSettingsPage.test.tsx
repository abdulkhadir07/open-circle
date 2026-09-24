import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { PersonalInfoSettingsPage } from './PersonalInfoSettingsPage';

describe('PersonalInfoSettingsPage', () => {
  it('shows personal info under a Back to Settings link', () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, authUser);
    useAuthStore.getState().setAuthenticated('access-token');

    renderWithProviders(<PersonalInfoSettingsPage />, { queryClient });

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('heading', { name: 'Personal info' })).toBeInTheDocument();
    expect(screen.getByText(`@${authUser.username}`)).toBeInTheDocument();
  });
});
