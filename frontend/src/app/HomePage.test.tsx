import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HomePage } from './HomePage';

describe('HomePage logout', () => {
  it('clears in-memory auth and cached user data after logout', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, authUser);
    useAuthStore.getState().setAuthenticated('access-token');
    const { user } = renderWithProviders(
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<p>Signed out</p>} />
      </Routes>,
      { queryClient },
    );

    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(await screen.findByText('Signed out')).toBeInTheDocument();
    expect(useAuthStore.getState().authStatus).toBe('anonymous');
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(queryClient.getQueryData(authQueryKeys.currentUser)).toBeUndefined();
  });
});
