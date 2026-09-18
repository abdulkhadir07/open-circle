import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { CreatePostDialog } from './CreatePostDialog';

function renderDialog(userOverrides: Partial<typeof authUser> = {}) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, ...userOverrides });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<CreatePostDialog />, { queryClient });
}

describe('CreatePostDialog', () => {
  it('creates a single invite post and closes the dialog', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('requires a group size to be chosen for a group invite', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(screen.getByLabelText("What's the invite?"), 'Beach day, who is in?');
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    expect(
      await screen.findByText('Group invites need a capacity of at least 2'),
    ).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('offers a dropdown of group sizes starting at 2', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));

    expect(screen.getByRole('option', { name: '2 people' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '1 people' })).not.toBeInTheDocument();
  });

  it('lets a group size beyond the dropdown be typed in directly', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(screen.getByLabelText("What's the invite?"), 'Big community cleanup day');
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));
    await user.click(screen.getByRole('option', { name: 'Custom number…' }));

    const customInput = screen.getByLabelText('How many people can join?');
    await user.type(customInput, '25');
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('lets a custom group size be abandoned back to the dropdown', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));
    await user.click(screen.getByRole('option', { name: 'Custom number…' }));
    await user.click(screen.getByRole('button', { name: 'Choose from list' }));

    expect(screen.getByRole('combobox', { name: 'How many people can join?' })).toBeInTheDocument();
  });

  it('only offers State as a scope when the poster has a verified state/region', async () => {
    const { user } = renderDialog({ verifiedStateRegion: undefined });

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('combobox', { name: 'Who can see it' }));

    expect(screen.getByRole('option', { name: 'City' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'State' })).not.toBeInTheDocument();
  });
});
