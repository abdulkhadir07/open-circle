import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { notification } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { NotificationBell } from './NotificationBell';

describe('NotificationBell', () => {
  it('shows no badge when there are no unread notifications', async () => {
    renderWithProviders(<NotificationBell />);

    expect(await screen.findByRole('button', { name: 'Notifications' })).toBeInTheDocument();
  });

  it('shows the exact unread count on the badge', async () => {
    server.use(
      http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 3 })),
    );
    renderWithProviders(<NotificationBell />);

    expect(
      await screen.findByRole('button', { name: 'Notifications, 3 unread' }),
    ).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('caps the badge display at 9+', async () => {
    server.use(
      http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 42 })),
    );
    renderWithProviders(<NotificationBell />);

    expect(await screen.findByText('9+')).toBeInTheDocument();
  });

  it('shows an empty state when there are no notifications to preview', async () => {
    const { user } = renderWithProviders(<NotificationBell />);

    await user.click(await screen.findByRole('button', { name: 'Notifications' }));

    expect(await screen.findByText("You're all caught up.")).toBeInTheDocument();
  });

  it('previews notifications from the inbox when the dropdown opens', async () => {
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
    const { user } = renderWithProviders(<NotificationBell />);

    await user.click(await screen.findByRole('button', { name: 'Notifications' }));

    expect(
      await screen.findByText(`${notification.actor?.username} requested to join your post`),
    ).toBeInTheDocument();
  });

  it('links to the full notifications page', async () => {
    const { user } = renderWithProviders(<NotificationBell />);

    await user.click(await screen.findByRole('button', { name: 'Notifications' }));

    expect(await screen.findByRole('link', { name: 'View all' })).toHaveAttribute(
      'href',
      '/notifications',
    );
  });

  it('shows a Mark all as read action only when there is something unread', async () => {
    server.use(
      http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 2 })),
    );
    const { user } = renderWithProviders(<NotificationBell />);

    await user.click(await screen.findByRole('button', { name: /Notifications, 2 unread/ }));

    expect(await screen.findByRole('button', { name: 'Mark all as read' })).toBeInTheDocument();
  });
});
