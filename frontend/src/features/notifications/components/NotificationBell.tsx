import { Bell } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';
import { useMarkAllNotificationsRead } from '../hooks/useMarkAllNotificationsRead';
import { useNotificationInbox } from '../hooks/useNotificationInbox';
import { useUnreadNotificationCount } from '../hooks/useUnreadNotificationCount';
import { NotificationRow } from './NotificationRow';

const PREVIEW_LIMIT = 5;

export function NotificationBell({ triggerClassName }: { triggerClassName?: string }) {
  const [open, setOpen] = useState(false);
  const unreadCount = useUnreadNotificationCount();
  const inbox = useNotificationInbox();
  const markAllRead = useMarkAllNotificationsRead();

  const notifications = (inbox.data?.pages[0]?.notifications ?? []).slice(0, PREVIEW_LIMIT);
  const count = unreadCount.data ?? 0;
  const badgeLabel = count > 9 ? '9+' : String(count);

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="outline"
          className={cn('justify-center', triggerClassName)}
          aria-label={count > 0 ? `Notifications, ${count} unread` : 'Notifications'}
        >
          <Bell aria-hidden="true" />
          Notifications
          {count > 0 ? (
            <span className="bg-primary text-primary-foreground ml-auto flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 text-xs font-semibold tabular-nums">
              {badgeLabel}
            </span>
          ) : null}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-80 p-0">
        <div className="flex items-center justify-between px-3 py-2.5">
          <span className="text-foreground text-sm font-semibold">Notifications</span>
          {count > 0 ? (
            <button
              type="button"
              onClick={() => markAllRead.mutate()}
              disabled={markAllRead.isPending}
              className="text-primary text-xs font-medium hover:underline disabled:opacity-50"
            >
              Mark all as read
            </button>
          ) : null}
        </div>

        {inbox.isLoading ? (
          <p className="text-muted-foreground px-3 py-4 text-center text-sm">Loading…</p>
        ) : notifications.length === 0 ? (
          <p className="text-muted-foreground px-3 py-4 text-center text-sm">
            You&apos;re all caught up.
          </p>
        ) : (
          <ScrollArea className="max-h-80">
            <div className="flex flex-col gap-0.5 px-1 pb-1">
              {notifications.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onNavigate={() => setOpen(false)}
                />
              ))}
            </div>
          </ScrollArea>
        )}

        <Link
          to="/notifications"
          onClick={() => setOpen(false)}
          className="text-primary border-border hover:bg-muted block border-t px-3 py-2.5 text-center text-sm font-medium"
        >
          View all
        </Link>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
