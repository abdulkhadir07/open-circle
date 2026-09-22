import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { notification } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { NotificationRow } from './NotificationRow';

describe('NotificationRow', () => {
  it("renders the actor's requested-to-join message", () => {
    renderWithProviders(<NotificationRow notification={notification} />);

    expect(
      screen.getByText(`${notification.actor?.username} requested to join your post`),
    ).toBeInTheDocument();
  });

  it('falls back to an actor-less message when there is no actor', () => {
    renderWithProviders(<NotificationRow notification={{ ...notification, actor: null }} />);

    expect(screen.getByText('Someone requested to join your post')).toBeInTheDocument();
  });

  it('pluralizes a repeated CHAT_ACTIVITY notification by its occurrence count', () => {
    renderWithProviders(
      <NotificationRow
        notification={{
          ...notification,
          type: 'CHAT_ACTIVITY',
          resource: { type: 'CHAT_ROOM', id: 'room-1' },
          occurrenceCount: 3,
        }}
      />,
    );

    expect(
      screen.getByText(`${notification.actor?.username} sent 3 new messages`),
    ).toBeInTheDocument();
  });

  it('shows an unread dot for an unread notification', () => {
    renderWithProviders(<NotificationRow notification={{ ...notification, read: false }} />);

    expect(screen.getByTestId('unread-dot')).toBeInTheDocument();
  });

  it('shows no unread dot for a read notification', () => {
    renderWithProviders(<NotificationRow notification={{ ...notification, read: true }} />);

    expect(screen.queryByTestId('unread-dot')).not.toBeInTheDocument();
  });

  it('renders as a link to the chat room for a CHAT_ACTIVITY notification', () => {
    renderWithProviders(
      <NotificationRow
        notification={{
          ...notification,
          type: 'CHAT_ACTIVITY',
          resource: { type: 'CHAT_ROOM', id: 'room-1' },
        }}
      />,
    );

    expect(screen.getByRole('link')).toHaveAttribute('href', '/chats/room-1');
  });

  it('renders as a plain button for a notification with nowhere to navigate', () => {
    renderWithProviders(
      <NotificationRow notification={{ ...notification, type: 'INVITE_POST_EXPIRED' }} />,
    );

    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('renders as a link to the ratings due tab for a RATING_REQUIRED notification', () => {
    renderWithProviders(
      <NotificationRow notification={{ ...notification, type: 'RATING_REQUIRED' }} />,
    );

    expect(screen.getByRole('link')).toHaveAttribute('href', '/ratings');
  });

  it('renders as a link to the ratings received tab for a RATING_REVEALED notification', () => {
    renderWithProviders(
      <NotificationRow notification={{ ...notification, type: 'RATING_REVEALED' }} />,
    );

    expect(screen.getByRole('link')).toHaveAttribute('href', '/ratings?tab=received');
  });

  it('marks an unread notification as read on click', async () => {
    const readSpy = vi.fn<(id: string) => void>();
    server.use(
      http.patch('*/api/notifications/:notificationId/read', ({ params }) => {
        readSpy(params.notificationId as string);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderWithProviders(
      <NotificationRow notification={{ ...notification, read: false }} />,
    );

    await user.click(screen.getByRole('link'));

    expect(readSpy).toHaveBeenCalledWith(notification.id);
  });

  it('does not call mark-as-read for an already-read notification', async () => {
    const readSpy = vi.fn<() => void>();
    server.use(
      http.patch('*/api/notifications/:notificationId/read', () => {
        readSpy();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderWithProviders(
      <NotificationRow notification={{ ...notification, read: true }} />,
    );

    await user.click(screen.getByRole('link'));

    expect(readSpy).not.toHaveBeenCalled();
  });

  it('calls onNavigate after handling the click', async () => {
    const onNavigate = vi.fn<() => void>();
    const { user } = renderWithProviders(
      <NotificationRow notification={{ ...notification, read: true }} onNavigate={onNavigate} />,
    );

    await user.click(screen.getByRole('link'));

    expect(onNavigate).toHaveBeenCalled();
  });
});
