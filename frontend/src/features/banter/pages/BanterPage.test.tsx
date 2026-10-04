import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse, delay } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, banter } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { BanterPage } from './BanterPage';

function renderBanterPage(initialEntries?: string[]) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<BanterPage />, { queryClient, initialEntries });
}

function page(items: (typeof banter)[], pageNumber = 0, totalPages = 1) {
  return { items, page: pageNumber, size: 20, totalElements: items.length, totalPages };
}

describe('BanterPage', () => {
  it('shows an empty state when nobody has posted yet', async () => {
    renderBanterPage();

    expect(await screen.findByText("It's quiet in here. Say something!")).toBeInTheDocument();
  });

  it('lists banter from the board', async () => {
    server.use(http.get('*/api/banter', () => HttpResponse.json(page([banter]))));
    renderBanterPage();

    expect(await screen.findByText(banter.content)).toBeInTheDocument();
  });

  it('starts on New and asks for the hot sort when the Hot tab is chosen', async () => {
    const requestedSorts: (string | null)[] = [];
    server.use(
      http.get('*/api/banter', ({ request }) => {
        requestedSorts.push(new URL(request.url).searchParams.get('sort'));
        return HttpResponse.json(page([banter]));
      }),
    );
    renderBanterPage();
    expect(screen.getByRole('link', { name: 'New' })).toHaveAttribute('aria-current', 'page');
    await screen.findByText(banter.content);
    expect(requestedSorts).toEqual(['new']);
  });

  it('opens on the Hot tab for ?sort=hot', async () => {
    const requestedSorts: (string | null)[] = [];
    server.use(
      http.get('*/api/banter', ({ request }) => {
        requestedSorts.push(new URL(request.url).searchParams.get('sort'));
        return HttpResponse.json(page([banter]));
      }),
    );
    renderBanterPage(['/banter?sort=hot']);

    expect(screen.getByRole('link', { name: 'Hot' })).toHaveAttribute('aria-current', 'page');
    await screen.findByText(banter.content);
    expect(requestedSorts).toEqual(['hot']);
  });

  it('loads more pages on request', async () => {
    const second = { ...banter, id: 'second-id', content: 'Second page banter' };
    server.use(
      http.get('*/api/banter', ({ request }) => {
        const pageNumber = Number(new URL(request.url).searchParams.get('page'));
        return HttpResponse.json(pageNumber === 0 ? page([banter], 0, 2) : page([second], 1, 2));
      }),
    );
    const { user } = renderBanterPage();

    await screen.findByText(banter.content);
    await user.click(screen.getByRole('button', { name: 'Load more' }));

    expect(await screen.findByText('Second page banter')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument();
  });

  it('shows the error when the board fails to load', async () => {
    server.use(
      http.get('*/api/banter', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Unable to load banter right now',
            path: '/api/banter',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );
    renderBanterPage();

    expect(await screen.findByText('Unable to load banter right now')).toBeInTheDocument();
  });

  it('flips the like immediately, before the server answers', async () => {
    server.use(
      http.get('*/api/banter', () => HttpResponse.json(page([banter]))),
      http.put('*/api/banter/:banterId/like', async () => {
        await delay(200);
        return HttpResponse.json({ likeCount: 3, likedByMe: true });
      }),
    );
    const { user } = renderBanterPage();
    await screen.findByText(banter.content);

    await user.click(screen.getByRole('button', { name: 'Like' }));

    // Optimistic: the heart and count change while the request is still in flight.
    const unlike = screen.getByRole('button', { name: 'Unlike' });
    expect(unlike).toHaveAttribute('aria-pressed', 'true');
    expect(unlike).toHaveTextContent('3');
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Unlike' })).toHaveTextContent('3'),
    );
  });

  it('rolls the like back when the request fails', async () => {
    server.use(
      http.get('*/api/banter', () => HttpResponse.json(page([banter]))),
      http.put('*/api/banter/:banterId/like', async () => {
        await delay(100);
        return HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'boom',
            path: '/api/banter/x/like',
            fieldErrors: {},
          },
          { status: 500 },
        );
      }),
    );
    const { user } = renderBanterPage();
    await screen.findByText(banter.content);

    await user.click(screen.getByRole('button', { name: 'Like' }));
    expect(screen.getByRole('button', { name: 'Unlike' })).toBeInTheDocument();

    const like = await screen.findByRole('button', { name: 'Like' });
    expect(like).toHaveAttribute('aria-pressed', 'false');
    expect(like).toHaveTextContent('2');
  });
});
