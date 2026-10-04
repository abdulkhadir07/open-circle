import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, invitePost } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ProfileOpenInvites } from './ProfileOpenInvites';

function renderInvites(
  props: { userId: string; isOwnProfile: boolean; displayName: string },
  user: typeof authUser = authUser,
) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, user);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<ProfileOpenInvites {...props} />, { queryClient });
}

const samPost = { ...invitePost, id: 'sam-post', posterId: 'sam-id', content: "Sam's hike" };

describe('ProfileOpenInvites', () => {
  it('lists your own open invites under "Your open invites"', async () => {
    renderInvites({ userId: authUser.id, isOwnProfile: true, displayName: 'Maya' });

    expect(await screen.findByText(invitePost.content)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Your open invites' })).toBeInTheDocument();
    expect(screen.getByText('Just one person · 1 spot left')).toBeInTheDocument();
    expect(screen.getByText(/h \d+m left$/)).toBeInTheDocument();
  });

  it("shows only that person's invites on someone else's profile", async () => {
    server.use(
      http.get('*/api/invite-posts/campus', () => HttpResponse.json([invitePost, samPost])),
    );
    renderInvites({ userId: 'sam-id', isOwnProfile: false, displayName: 'Sam' });

    expect(await screen.findByText("Sam's hike")).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: "Sam's open invites" })).toBeInTheDocument();
    expect(screen.queryByText(invitePost.content)).not.toBeInTheDocument();
  });

  it('shows only the three newest open invites', async () => {
    const many = Array.from({ length: 5 }, (_, index) => ({
      ...samPost,
      id: `sam-${index}`,
      content: `Sam invite ${index}`,
      createdAt: new Date(Date.now() + index * 60_000).toISOString(),
    }));
    server.use(http.get('*/api/invite-posts/campus', () => HttpResponse.json(many)));
    renderInvites({ userId: 'sam-id', isOwnProfile: false, displayName: 'Sam' });

    expect(await screen.findByText('Sam invite 4')).toBeInTheDocument();
    expect(screen.getByText('Sam invite 3')).toBeInTheDocument();
    expect(screen.getByText('Sam invite 2')).toBeInTheDocument();
    expect(screen.queryByText('Sam invite 1')).not.toBeInTheDocument();
    expect(screen.queryByText('Sam invite 0')).not.toBeInTheDocument();
  });

  it('links to the new-post page when you have nothing open', async () => {
    server.use(http.get('*/api/invite-posts/campus', () => HttpResponse.json([])));
    renderInvites({ userId: authUser.id, isOwnProfile: true, displayName: 'Maya' });

    expect(await screen.findByText(/Nothing open right now\./)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start an invite' })).toHaveAttribute('href', '/new');
  });

  it("shows a plain empty state on someone else's profile", async () => {
    renderInvites({ userId: 'sam-id', isOwnProfile: false, displayName: 'Sam' });

    expect(await screen.findByText('Nothing open right now.')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Start an invite' })).not.toBeInTheDocument();
  });
});
