import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, dueRating, receivedRating, reputation } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { RatingsPage } from './RatingsPage';

function renderRatingsPage(initialEntries?: string[]) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<RatingsPage />, { queryClient, initialEntries });
}

describe('RatingsPage', () => {
  it('shows an empty state on the Due tab by default', async () => {
    renderRatingsPage();

    expect(await screen.findByText('Nothing to rate right now.')).toBeInTheDocument();
  });

  it('shows a due rating with a Rate action', async () => {
    server.use(http.get('*/api/users/me/ratings/due', () => HttpResponse.json([dueRating])));

    renderRatingsPage();

    expect(await screen.findByText(dueRating.otherUsername)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Rate' })).toBeInTheDocument();
  });

  it('shows an error message when the due list fails to load', async () => {
    server.use(
      http.get('*/api/users/me/ratings/due', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Unable to load ratings due right now',
            path: '/api/users/me/ratings/due',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );

    renderRatingsPage();

    expect(await screen.findByText('Unable to load ratings due right now')).toBeInTheDocument();
  });

  it('switches to the Received tab and shows an empty state', async () => {
    const { user } = renderRatingsPage();

    await user.click(screen.getByRole('radio', { name: 'Received' }));

    expect(await screen.findByText('No ratings yet.')).toBeInTheDocument();
  });

  it('starts on the Received tab when the URL asks for it', async () => {
    renderRatingsPage(['/ratings?tab=received']);

    expect(await screen.findByText('No ratings yet.')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Received' })).toHaveAttribute('aria-checked', 'true');
  });

  it('shows received ratings and the reputation summary', async () => {
    server.use(
      http.get('*/api/users/me/ratings/received', () =>
        HttpResponse.json({
          ratings: [receivedRating],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        }),
      ),
      http.get('*/api/users/:userId/reputation', () => HttpResponse.json(reputation)),
    );

    const { user } = renderRatingsPage();
    await user.click(screen.getByRole('radio', { name: 'Received' }));

    expect(await screen.findByText(receivedRating.raterUsername)).toBeInTheDocument();
    expect(screen.getByText(/average from/)).toBeInTheDocument();
  });

  it('shows Load more when another page is available, and hides it once all are loaded', async () => {
    const firstPage = {
      ratings: [receivedRating],
      page: 0,
      size: 1,
      totalElements: 2,
      totalPages: 2,
    };
    const secondPage = {
      ratings: [{ ...receivedRating, id: 'second-id' }],
      page: 1,
      size: 1,
      totalElements: 2,
      totalPages: 2,
    };
    server.use(
      http.get('*/api/users/me/ratings/received', ({ request }) => {
        const page = new URL(request.url).searchParams.get('page');
        return HttpResponse.json(page === '1' ? secondPage : firstPage);
      }),
    );
    const { user } = renderRatingsPage();
    await user.click(screen.getByRole('radio', { name: 'Received' }));

    const loadMoreButton = await screen.findByRole('button', { name: 'Load more' });
    await user.click(loadMoreButton);

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument(),
    );
  });
});
