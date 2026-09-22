import { ArrowLeft, LoaderCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
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
    <div className="mx-auto max-w-2xl">
      <Link
        to="/"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back
      </Link>

      <div className="mt-4 flex items-center justify-between gap-4">
        <div>
          <p className="text-primary text-sm font-semibold">Your circle</p>
          <h1 className="text-foreground text-3xl font-semibold">Notifications</h1>
        </div>
        {count > 0 ? (
          <Button
            type="button"
            variant="outline"
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending}
          >
            Mark all as read
          </Button>
        ) : null}
      </div>

      <div className="mt-6">
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
          <p className="text-muted-foreground text-base">You&apos;re all caught up.</p>
        ) : (
          <div className="flex flex-col gap-1">
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
