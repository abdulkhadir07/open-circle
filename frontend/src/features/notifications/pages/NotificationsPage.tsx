import { Bell, LoaderCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { PageHeader } from '@/components/ui/page-header';
import { NotificationRow } from '../components/NotificationRow';
import { useMarkAllNotificationsRead } from '../hooks/useMarkAllNotificationsRead';
import { useNotificationInbox } from '../hooks/useNotificationInbox';
import { useUnreadNotificationCount } from '../hooks/useUnreadNotificationCount';

export function NotificationsPage() {
  const inbox = useNotificationInbox();
  const unreadCount = useUnreadNotificationCount();
  const markAllRead = useMarkAllNotificationsRead();

  const notifications = inbox.data?.pages.flatMap((page) => page.notifications) ?? [];
  const count = unreadCount.data ?? 0;

  return (
    <div>
      <div className="flex items-start justify-between gap-4">
        <PageHeader title="Notifications" sub="Requests, accepted invites, and ratings." />
        {count > 0 ? (
          <Button
            type="button"
            variant="outline"
            className="shrink-0"
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending}
          >
            Mark all as read
          </Button>
        ) : null}
      </div>

      <div>
        {inbox.isLoading ? (
          <LoaderCircle
            aria-hidden="true"
            className="text-muted-foreground mx-auto block size-5 animate-spin"
          />
        ) : inbox.isError ? (
          <p role="alert" className="text-destructive text-base">
            {inbox.error instanceof Error ? inbox.error.message : 'Unable to load notifications.'}
          </p>
        ) : notifications.length === 0 ? (
          <EmptyState icon={Bell}>You&apos;re all caught up.</EmptyState>
        ) : (
          <div className="flex flex-col gap-2">
            {notifications.map((notification) => (
              <NotificationRow key={notification.id} notification={notification} />
            ))}
          </div>
        )}

        {inbox.hasNextPage ? (
          <div className="mt-4 flex justify-center">
            <Button
              type="button"
              variant="outline"
              onClick={() => void inbox.fetchNextPage()}
              disabled={inbox.isFetchingNextPage}
            >
              {inbox.isFetchingNextPage ? (
                <LoaderCircle aria-hidden="true" className="animate-spin" />
              ) : null}
              Load more
            </Button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
