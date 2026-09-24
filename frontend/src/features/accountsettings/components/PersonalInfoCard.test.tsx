import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { PersonalInfoCard } from './PersonalInfoCard';

function renderCard() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<PersonalInfoCard />, { queryClient });
}

describe('PersonalInfoCard', () => {
  it('shows the name, username, phone number, and date of birth', () => {
    renderCard();

    expect(screen.getByText(`${authUser.firstName} ${authUser.lastName}`)).toBeInTheDocument();
    expect(screen.getByText(`@${authUser.username}`)).toBeInTheDocument();
    expect(screen.getByText(authUser.phoneNumber!)).toBeInTheDocument();
    expect(screen.getByText('May 12, 1994')).toBeInTheDocument();
  });
});
