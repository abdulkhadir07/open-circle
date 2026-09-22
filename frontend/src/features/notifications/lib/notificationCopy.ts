import type { Notification, NotificationType } from '../api/contracts';

function actorPhrase(notification: Notification, withActor: string, withoutActor: string): string {
  const actor = notification.actor?.username;
  return actor ? `${actor} ${withActor}` : withoutActor;
}

/**
 * There's no per-item detail page for a single engagement request or invite
 * post yet, and no ratings UI at all — so only CHAT_ACTIVITY (a real chat
 * room) gets precise navigation. Engagement types go to the requests list;
 * rating and expiration types have nowhere to send the user yet.
 */
const ROUTE_BUILDERS: Record<NotificationType, (notification: Notification) => string | null> = {
  ENGAGEMENT_REQUESTED: () => '/requests',
  ENGAGEMENT_ACCEPTED: () => '/requests',
  ENGAGEMENT_DECLINED: () => '/requests',
  ENGAGEMENT_HELD: () => '/requests',
  ENGAGEMENT_WITHDRAWN: () => '/requests',
  INVITE_POST_EXPIRED: () => null,
  ENGAGEMENT_REQUEST_EXPIRED: () => null,
  CHAT_ACTIVITY: (notification) => `/chats/${notification.resource.id}`,
  RATING_REQUIRED: () => null,
  RATING_REVEALED: () => null,
};

const MESSAGE_BUILDERS: Record<NotificationType, (notification: Notification) => string> = {
  ENGAGEMENT_REQUESTED: (n) =>
    actorPhrase(n, 'requested to join your post', 'Someone requested to join your post'),
  ENGAGEMENT_ACCEPTED: (n) => actorPhrase(n, 'accepted your request', 'Your request was accepted'),
  ENGAGEMENT_DECLINED: (n) => actorPhrase(n, 'declined your request', 'Your request was declined'),
  ENGAGEMENT_HELD: (n) =>
    actorPhrase(n, 'put your request on hold', 'Your request was put on hold'),
  ENGAGEMENT_WITHDRAWN: (n) => actorPhrase(n, 'withdrew their request', 'A request was withdrawn'),
  INVITE_POST_EXPIRED: () => 'Your invite post expired',
  ENGAGEMENT_REQUEST_EXPIRED: () => 'Your request expired',
  CHAT_ACTIVITY: (n) => {
    const actor = n.actor?.username;
    if (n.occurrenceCount > 1) {
      return actor
        ? `${actor} sent ${n.occurrenceCount} new messages`
        : `${n.occurrenceCount} new messages`;
    }
    return actor ? `${actor} sent a new message` : 'New message';
  },
  RATING_REQUIRED: () => 'Rate your recent hangout',
  RATING_REVEALED: () => 'A rating about you was revealed',
};

export function messageFor(notification: Notification): string {
  return MESSAGE_BUILDERS[notification.type](notification);
}

export function routeFor(notification: Notification): string | null {
  return ROUTE_BUILDERS[notification.type](notification);
}
