import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, userProfile } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ProfilePage } from './ProfilePage';

function renderProfilePage(userId: string) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/profile/:userId" element={<ProfilePage />} />
    </Routes>,
    { queryClient, initialEntries: [`/profile/${userId}`] },
  );
}

describe('ProfilePage', () => {
  it("shows an Edit profile button on the current user's own profile", async () => {
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)));

    renderProfilePage(authUser.id);

    expect(await screen.findByText(userProfile.displayName)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit profile' })).toBeInTheDocument();
  });

  it('shows no Edit profile button when viewing someone else', async () => {
    const otherProfile = { ...userProfile, userId: 'someone-else', username: 'someone_else' };
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(otherProfile)));

    renderProfilePage('someone-else');

    await screen.findByText(otherProfile.displayName);
    expect(screen.queryByRole('button', { name: 'Edit profile' })).not.toBeInTheDocument();
  });

  it('shows the real name to the profile owner, marked as private', async () => {
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)));

    renderProfilePage(authUser.id);

    expect(await screen.findByText(`${authUser.firstName} ${authUser.lastName}`)).toHaveAttribute(
      'title',
      'Only visible to you',
    );
  });

  it("hides the real name when viewing someone else's profile", async () => {
    const otherProfile = { ...userProfile, userId: 'someone-else', username: 'someone_else' };
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(otherProfile)));

    renderProfilePage('someone-else');

    await screen.findByText(otherProfile.displayName);
    expect(
      screen.queryByText(`${authUser.firstName} ${authUser.lastName}`),
    ).not.toBeInTheDocument();
  });

  it('shows bio, interests, reputation, and awards', async () => {
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)));

    renderProfilePage(authUser.id);

    expect(await screen.findByText(userProfile.bio!)).toBeInTheDocument();
    expect(screen.getByText('Hiking')).toBeInTheDocument();
    expect(screen.getByText(/4\.8\/5/)).toBeInTheDocument();
    expect(screen.getByText('Circle Champion 2025')).toBeInTheDocument();
  });

  it('shows a not-found message for a 404 profile', async () => {
    server.use(
      http.get('*/api/users/:userId/profile', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 404,
            error: 'NOT_FOUND',
            message: 'User profile not found',
            path: '/api/users/missing/profile',
            fieldErrors: {},
          },
          { status: 404 },
        ),
      ),
    );

    renderProfilePage('missing-user');

    expect(await screen.findByText("This profile doesn't exist.")).toBeInTheDocument();
  });

  it('edits and saves the display name, bio, and interests', async () => {
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)));

    const { user } = renderProfilePage(authUser.id);

    await user.click(await screen.findByRole('button', { name: 'Edit profile' }));

    const displayNameInput = screen.getByLabelText('Display name');
    await user.clear(displayNameInput);
    await user.type(displayNameInput, 'New Name');

    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument(),
    );
    expect(screen.getByText('New Name')).toBeInTheDocument();
  });

  it('cancels editing without saving', async () => {
    server.use(http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)));

    const { user } = renderProfilePage(authUser.id);

    await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
    await user.clear(screen.getByLabelText('Display name'));
    await user.type(screen.getByLabelText('Display name'), 'Abandoned Edit');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByText(userProfile.displayName)).toBeInTheDocument();
    expect(screen.queryByText('Abandoned Edit')).not.toBeInTheDocument();
  });

  it('shows a validation error from the backend when saving fails', async () => {
    server.use(
      http.get('*/api/users/:userId/profile', () => HttpResponse.json(userProfile)),
      http.put('*/api/users/me/profile', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Interests must be unique ignoring case',
            path: '/api/users/me/profile',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );

    const { user } = renderProfilePage(authUser.id);

    await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Interests must be unique ignoring case')).toBeInTheDocument();
    // The form should still be open so the user can fix it.
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument();
  });
});
