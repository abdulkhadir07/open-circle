import { Link } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';
import type { Notification } from '../api/contracts';
import { useMarkNotificationRead } from '../hooks/useMarkNotificationRead';
import { formatRelativeTime } from '../lib/formatRelativeTime';
import { messageFor, routeFor } from '../lib/notificationCopy';

type NotificationRowProps = {
  notification: Notification;
  /** Called after the click handler runs — e.g. to close the dropdown. */
  onNavigate?: () => void;
};

export function NotificationRow({ notification, onNavigate }: NotificationRowProps) {
  const markRead = useMarkNotificationRead();
  const route = routeFor(notification);

  function handleClick() {
    if (!notification.read) markRead.mutate(notification.id);
    onNavigate?.();
  }

  const className = cn(
    'bg-card hover:border-primary/40 flex w-full items-start gap-3 rounded-2xl border p-3 text-left transition',
    !notification.read && 'border-primary/30 bg-primary/5',
  );

  const content = (
    <>
      <Avatar
        aria-hidden="true"
        name={notification.actor?.username ?? '?'}
        profileImage={notification.actor?.profileImage}
        className="size-8 text-sm"
      />
      <div className="min-w-0 flex-1">
        <p className="text-foreground text-sm leading-5">{messageFor(notification)}</p>
        <p className="text-muted-foreground mt-0.5 text-xs">
          {formatRelativeTime(notification.occurredAt)}
        </p>
      </div>
      {!notification.read ? (
        <span
          aria-hidden="true"
          className="bg-primary mt-1.5 size-2 shrink-0 rounded-full"
          data-testid="unread-dot"
        />
      ) : null}
    </>
  );

  if (route) {
    return (
      <Link to={route} onClick={handleClick} className={className}>
        {content}
      </Link>
    );
  }

  return (
    <button type="button" onClick={handleClick} className={className}>
      {content}
    </button>
  );
}
