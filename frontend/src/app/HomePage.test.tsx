import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, invitePost } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HomePage } from './HomePage';

function renderHomePage() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<p>Signed out</p>} />
    </Routes>,
    { queryClient },
  );
}

describe('HomePage feed', () => {
  it('shows the local feed by default and switches to the global feed', async () => {
    const { user } = renderHomePage();

    expect(await screen.findByText(invitePost.content)).toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: 'Global' }));
    expect(
      await screen.findByText('No invite posts here yet. Be the first to post one.'),
    ).toBeInTheDocument();
  });

  it('opens the create-post dialog from the New post button', async () => {
    const { user } = renderHomePage();
    await screen.findByText(invitePost.content);

    // Two "New post" triggers exist in the DOM at once — one for the
    // desktop rail, one for the mobile inline layout — toggled with
    // CSS breakpoints that jsdom doesn't evaluate, so both are "visible"
    // to queries here. Either one opens the same dialog.
    await user.click(screen.getAllByRole('button', { name: 'New post' })[0]!);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});

describe('HomePage location gating', () => {
  it('never requests the feed before location is verified, and loads it fresh once it is', async () => {
    const requestedPaths: string[] = [];
    const onRequestStart = ({ request }: { request: Request }) => {
      requestedPaths.push(new URL(request.url).pathname);
    };
    server.events.on('request:start', onRequestStart);

    const queryClient = createTestQueryClient();
    queryClient.setQueryData(authQueryKeys.currentUser, {
      ...authUser,
      locationVerifiedAt: undefined,
    });
    useAuthStore.getState().setAuthenticated('access-token');
    renderWithProviders(
      <Routes>
        <Route path="/" element={<HomePage />} />
      </Routes>,
      { queryClient },
    );

    expect(
      await screen.findByRole('heading', { name: 'Verify your location' }),
    ).toBeInTheDocument();
    expect(requestedPaths.some((path) => path.includes('/invite-posts/'))).toBe(false);

    // Mirrors exactly what useVerifyLocation's onSuccess does — no feed
    // request should have been made before this point.
    queryClient.setQueryData(authQueryKeys.currentUser, authUser);

    expect(await screen.findByText(invitePost.content)).toBeInTheDocument();
    await waitFor(() => {
      expect(requestedPaths.filter((path) => path.includes('/invite-posts/local')).length).toBe(1);
    });

    server.events.removeListener('request:start', onRequestStart);
  });
});

describe('HomePage engagement requests', () => {
  it('links to the dedicated Requests page', async () => {
    renderHomePage();
    await screen.findByText(invitePost.content);

    expect(screen.getAllByRole('link', { name: 'Requests' })[0]).toHaveAttribute(
      'href',
      '/requests',
    );
  });

  it("shows no inline request management on the current user's own post", async () => {
    renderHomePage();
    await screen.findByText(invitePost.content);

    expect(screen.queryByRole('button', { name: 'Engage' })).not.toBeInTheDocument();
  });

  it("shows an Engage control for another user's post", async () => {
    server.use(
      http.get('*/api/invite-posts/local', () =>
        HttpResponse.json([
          {
            ...invitePost,
            id: 'someone-elses-post',
            posterId: 'someone-else',
            posterUsername: 'sam.rivera',
          },
        ]),
      ),
    );
    renderHomePage();

    await screen.findByText(invitePost.content);

    expect(screen.getByRole('button', { name: 'Engage' })).toBeInTheDocument();
  });
});

describe('HomePage logout', () => {
  it('clears in-memory auth and cached user data after logout', async () => {
    const { user, queryClient } = renderHomePage();
    await screen.findByText(invitePost.content);

    // Same as the "New post" button above — two Sign out triggers exist at
    // once (desktop rail + mobile inline), both visible to jsdom's queries.
    await user.click(screen.getAllByRole('button', { name: 'Sign out' })[0]!);
    expect(await screen.findByText('Signed out')).toBeInTheDocument();
    expect(useAuthStore.getState().authStatus).toBe('anonymous');
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(queryClient.getQueryData(authQueryKeys.currentUser)).toBeUndefined();
  });
});
