import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { AppLayout } from './AppLayout';

function renderLayout(path = '/requests') {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<p>Login page</p>} />
      <Route
        path="*"
        element={
          <AppLayout>
            <p>Page content</p>
          </AppLayout>
        }
      />
    </Routes>,
    { queryClient, initialEntries: [path] },
  );
}

describe('AppLayout', () => {
  it('renders the page content', () => {
    renderLayout();

    expect(screen.getByText('Page content')).toBeInTheDocument();
  });

  it('links to every main section', () => {
    renderLayout();

    // Desktop sidebar and mobile header both render, toggled by CSS breakpoints jsdom doesn't evaluate.
    for (const [label, href] of [
      ['Home', '/'],
      ['Requests', '/requests'],
      ['Chats', '/chats'],
      ['Notifications', '/notifications'],
      ['Ratings', '/ratings'],
      ['Scoreboard', '/scoreboard'],
      ['Profile', `/profile/${authUser.id}`],
      ['Settings', '/settings'],
    ] as const) {
      expect(screen.getAllByRole('link', { name: label })[0]).toHaveAttribute('href', href);
    }
  });

  it("marks the current section's link as the current page", () => {
    renderLayout('/ratings');

    expect(screen.getAllByRole('link', { name: 'Ratings' })[0]).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getAllByRole('link', { name: 'Requests' })[0]).not.toHaveAttribute(
      'aria-current',
    );
  });

  it('shows the current user in the sidebar without their email', () => {
    renderLayout();

    expect(screen.getByText(`${authUser.firstName} ${authUser.lastName}`)).toBeInTheDocument();
    expect(screen.getByText(`@${authUser.username}`)).toBeInTheDocument();
    expect(screen.queryByText(authUser.email)).not.toBeInTheDocument();
  });

  it('shows the unread notification count on the Notifications link', async () => {
    server.use(
      http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 3 })),
    );
    renderLayout();

    expect(
      (await screen.findAllByRole('link', { name: 'Notifications, 3 unread' }))[0],
    ).toBeInTheDocument();
  });

  it('links Start an invite to the new-post page', () => {
    renderLayout();

    for (const link of screen.getAllByRole('link', { name: 'Start an invite' })) {
      expect(link).toHaveAttribute('href', '/new');
    }
  });

  it('shows the floating create button except on the new-post page and inside a chat room', () => {
    const { unmount } = renderLayout('/requests');
    expect(screen.getAllByRole('link', { name: 'Start an invite' })).toHaveLength(2);
    unmount();

    const chat = renderLayout('/chats/room-1');
    expect(screen.getAllByRole('link', { name: 'Start an invite' })).toHaveLength(1);
    chat.unmount();

    renderLayout('/new');
    expect(screen.getAllByRole('link', { name: 'Start an invite' })).toHaveLength(1);
  });

  it('logs out from the sidebar and returns to the login page', async () => {
    const { user } = renderLayout();

    await user.click(screen.getAllByRole('button', { name: 'Log out' })[0]!);

    expect(await screen.findByText('Login page')).toBeInTheDocument();
  });
});
