import { screen, waitFor, within } from '@testing-library/react';
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

  it('greets the user and counts open invites near their verified location', async () => {
    renderHomePage();

    expect(
      await screen.findByRole('heading', { name: /^(Morning|Hey|Evening), Maya$/ }),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(
        '1 open invite near San Francisco, United States right now. Jump in.',
      ),
    ).toBeInTheDocument();
  });
});

describe('HomePage composer and search', () => {
  it('links the composer prompt to the new-post page', async () => {
    renderHomePage();
    await screen.findByText(invitePost.content);

    expect(screen.getByRole('link', { name: /What do you want to do today/ })).toHaveAttribute(
      'href',
      '/new',
    );
  });

  it('links each idea chip to the new-post page with its text prefilled', async () => {
    renderHomePage();
    await screen.findByText(invitePost.content);

    const href = screen.getByRole('link', { name: 'Grab food' }).getAttribute('href');
    expect(href).toBe(
      `/new?text=${encodeURIComponent('Anyone want to grab lunch downtown at noon? 2 people')}&tags=food`,
    );
  });

  it('filters the feed by search text and offers a way back', async () => {
    const { user } = renderHomePage();
    await screen.findByText(invitePost.content);

    await user.type(screen.getByRole('searchbox', { name: 'Search invites' }), 'zzzz');
    expect(await screen.findByText('No invites match that. Try another word.')).toBeInTheDocument();
    expect(screen.queryByText(invitePost.content)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Clear search' }));
    expect(await screen.findByText(invitePost.content)).toBeInTheDocument();
  });

  it('matches search text against the poster and content', async () => {
    const { user } = renderHomePage();
    await screen.findByText(invitePost.content);

    await user.type(screen.getByRole('searchbox', { name: 'Search invites' }), 'COFFEE');

    expect(screen.getByText(invitePost.content)).toBeInTheDocument();
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

describe('HomePage topics', () => {
  const walkPost = {
    ...invitePost,
    id: 'post-walk',
    content: 'Walk the bay',
    tags: ['walk', 'outdoors'],
  };
  const codePost = {
    ...invitePost,
    id: 'post-code',
    content: 'Pair on a side project',
    tags: ['code'],
  };

  function useTaggedFeed() {
    server.use(http.get('*/api/invite-posts/local', () => HttpResponse.json([walkPost, codePost])));
  }

  it('lists topic filters from the loaded feed', async () => {
    useTaggedFeed();
    renderHomePage();
    await screen.findByText('Walk the bay');

    const filters = screen.getByRole('group', { name: 'Filter by topic' });
    expect(within(filters).getByRole('button', { name: '#walk' })).toBeInTheDocument();
    expect(within(filters).getByRole('button', { name: '#code' })).toBeInTheDocument();
    expect(within(filters).getByRole('button', { name: '#outdoors' })).toBeInTheDocument();
  });

  it('filters the feed by a topic and clears the filter on a second click', async () => {
    useTaggedFeed();
    const { user } = renderHomePage();
    await screen.findByText('Walk the bay');

    const filters = screen.getByRole('group', { name: 'Filter by topic' });
    await user.click(within(filters).getByRole('button', { name: '#code' }));

    expect(screen.getByText('Pair on a side project')).toBeInTheDocument();
    expect(screen.queryByText('Walk the bay')).not.toBeInTheDocument();

    await user.click(within(filters).getByRole('button', { name: '#code' }));
    expect(await screen.findByText('Walk the bay')).toBeInTheDocument();
  });

  it('filters by the topic chip on a card', async () => {
    useTaggedFeed();
    const { user } = renderHomePage();
    await screen.findByText('Walk the bay');

    const card = screen.getByText('Walk the bay').closest('article')!;
    await user.click(within(card).getByRole('button', { name: '#outdoors' }));

    expect(screen.queryByText('Pair on a side project')).not.toBeInTheDocument();
    expect(screen.getByText('Walk the bay')).toBeInTheDocument();
  });

  it('matches search text against topics', async () => {
    useTaggedFeed();
    const { user } = renderHomePage();
    await screen.findByText('Walk the bay');

    await user.type(screen.getByRole('searchbox', { name: 'Search invites' }), 'code');

    expect(screen.getByText('Pair on a side project')).toBeInTheDocument();
    expect(screen.queryByText('Walk the bay')).not.toBeInTheDocument();
  });

  it('shows no topic filters when no post has topics', async () => {
    renderHomePage();
    await screen.findByText(invitePost.content);

    expect(screen.queryByRole('group', { name: 'Filter by topic' })).not.toBeInTheDocument();
  });
});

describe('HomePage own invites', () => {
  const othersPost = {
    ...invitePost,
    id: 'others',
    posterId: 'sam-id',
    posterUsername: 'sam',
    content: "Sam's hike",
    createdAt: new Date(Date.now() + 60_000).toISOString(),
  };

  it('puts your open invites first under their own heading, then everyone else under Nearby', async () => {
    server.use(
      http.get('*/api/invite-posts/local', () => HttpResponse.json([othersPost, invitePost])),
    );
    renderHomePage();

    expect(await screen.findByRole('heading', { name: 'Your open invites' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nearby' })).toBeInTheDocument();

    const mine = screen.getByText(invitePost.content);
    const theirs = screen.getByText("Sam's hike");
    expect(mine.compareDocumentPosition(theirs) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('adds no group headings when you have no open invites', async () => {
    server.use(http.get('*/api/invite-posts/local', () => HttpResponse.json([othersPost])));
    renderHomePage();

    await screen.findByText("Sam's hike");
    expect(screen.queryByRole('heading', { name: 'Your open invites' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Nearby' })).not.toBeInTheDocument();
  });

  it('skips the Nearby heading when only your own invites are showing', async () => {
    renderHomePage();

    expect(await screen.findByRole('heading', { name: 'Your open invites' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Nearby' })).not.toBeInTheDocument();
  });
});
