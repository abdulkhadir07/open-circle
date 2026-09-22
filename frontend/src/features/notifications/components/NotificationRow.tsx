import { Link } from 'react-router-dom';
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
    'flex w-full items-start gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-muted',
    !notification.read && 'bg-primary/5',
  );

  const content = (
    <>
      <span
        aria-hidden="true"
        className="bg-accent/20 text-accent mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
      >
        {(notification.actor?.username ?? '?')[0]?.toUpperCase()}
      </span>
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
