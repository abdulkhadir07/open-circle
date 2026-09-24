import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { PinSettingsPage } from './PinSettingsPage';

describe('PinSettingsPage', () => {
  it('shows the PIN form under a Back to Settings link', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, {
      ...authUser,
      hasHiddenChatsPin: false,
    });
    useAuthStore.getState().setAuthenticated('access-token');

    renderWithProviders(<PinSettingsPage />, { queryClient });

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('heading', { name: 'Hidden chats PIN' })).toBeInTheDocument();
    expect(await screen.findByLabelText('PIN')).toBeInTheDocument();
  });
});
