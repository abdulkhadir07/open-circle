import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { notification } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { NotificationsPage } from './NotificationsPage';

describe('NotificationsPage', () => {
  it('shows an empty state when there are no notifications', async () => {
    renderWithProviders(<NotificationsPage />);

    expect(await screen.findByText("You're all caught up.")).toBeInTheDocument();
  });

  it('shows an error message when the inbox fails to load', async () => {
    server.use(
      http.get('*/api/notifications', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Something went wrong loading notifications',
            path: '/api/notifications',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );

    renderWithProviders(<NotificationsPage />);

    expect(
      await screen.findByText('Something went wrong loading notifications'),
    ).toBeInTheDocument();
  });

  it('renders every notification returned by the inbox', async () => {
    server.use(
      http.get('*/api/notifications', () =>
        HttpResponse.json({
          notifications: [notification],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        }),
      ),
    );

    renderWithProviders(<NotificationsPage />);

    expect(
      await screen.findByText(`${notification.actor?.username} requested to join your post`),
    ).toBeInTheDocument();
  });

  it('shows a Load more button when another page is available, and hides it once all are loaded', async () => {
    const firstPage = {
      notifications: [notification],
      page: 0,
      size: 1,
      totalElements: 2,
      totalPages: 2,
    };
    const secondPage = {
      notifications: [{ ...notification, id: 'second-id' }],
      page: 1,
      size: 1,
      totalElements: 2,
      totalPages: 2,
    };
    server.use(
      http.get('*/api/notifications', ({ request }) => {
        const page = new URL(request.url).searchParams.get('page');
        return HttpResponse.json(page === '1' ? secondPage : firstPage);
      }),
    );
    const { user } = renderWithProviders(<NotificationsPage />);

    const loadMoreButton = await screen.findByRole('button', { name: 'Load more' });
    await user.click(loadMoreButton);

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument(),
    );
  });

  it('shows Mark all as read only when there is something unread, and calls the endpoint on click', async () => {
    const markAllSpy = vi.fn<() => void>();
    server.use(
      http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 2 })),
      http.patch('*/api/notifications/read-all', () => {
        markAllSpy();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderWithProviders(<NotificationsPage />);

    const markAllButton = await screen.findByRole('button', { name: 'Mark all as read' });
    await user.click(markAllButton);

    expect(markAllSpy).toHaveBeenCalled();
  });

  it('does not show Mark all as read when there is nothing unread', async () => {
    renderWithProviders(<NotificationsPage />);

    await screen.findByText("You're all caught up.");
    expect(screen.queryByRole('button', { name: 'Mark all as read' })).not.toBeInTheDocument();
  });
});
