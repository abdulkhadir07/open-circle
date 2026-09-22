import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

export const NOTIFICATION_TYPES = [
  'ENGAGEMENT_REQUESTED',
  'ENGAGEMENT_ACCEPTED',
  'ENGAGEMENT_DECLINED',
  'ENGAGEMENT_HELD',
  'ENGAGEMENT_WITHDRAWN',
  'INVITE_POST_EXPIRED',
  'ENGAGEMENT_REQUEST_EXPIRED',
  'CHAT_ACTIVITY',
  'RATING_REQUIRED',
  'RATING_REVEALED',
] as const;

export type NotificationType = (typeof NOTIFICATION_TYPES)[number];

export const NOTIFICATION_RESOURCE_TYPES = [
  'ENGAGEMENT_REQUEST',
  'INVITE_POST',
  'CHAT_ROOM',
] as const;

export type NotificationResourceType = (typeof NOTIFICATION_RESOURCE_TYPES)[number];

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const notificationActorSchema = z
  .object({
    userId: z.string().min(1),
    username: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
  })
  .passthrough();

export type NotificationActor = z.infer<typeof notificationActorSchema>;

const notificationResourceSchema = z
  .object({
    type: z.enum(NOTIFICATION_RESOURCE_TYPES),
    id: z.string().min(1),
  })
  .passthrough();

export type NotificationResource = z.infer<typeof notificationResourceSchema>;

const notificationSchema = z
  .object({
    id: z.string().min(1),
    type: z.enum(NOTIFICATION_TYPES),
    actor: notificationActorSchema.nullish(),
    resource: notificationResourceSchema,
    context: notificationResourceSchema.nullish(),
    occurrenceCount: z.number(),
    occurredAt: z.string().min(1),
    read: z.boolean(),
    readAt: z.string().nullish(),
  })
  .passthrough();

export type Notification = z.infer<typeof notificationSchema>;

const notificationInboxSchema = z
  .object({
    notifications: z.array(notificationSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type NotificationInbox = z.infer<typeof notificationInboxSchema>;

const unreadCountSchema = z
  .object({
    unreadCount: z.number(),
  })
  .passthrough();

const notificationEventSchema = z
  .object({
    notification: notificationSchema,
    unreadCount: z.number(),
  })
  .passthrough();

export type NotificationEvent = z.infer<typeof notificationEventSchema>;

export function parseNotification(value: unknown): Notification {
  return parseWithContract(notificationSchema, value);
}

export function parseNotificationEvent(value: unknown): NotificationEvent {
  return parseWithContract(notificationEventSchema, value);
}

export function parseNotificationInbox(value: unknown): NotificationInbox {
  return parseWithContract(notificationInboxSchema, value);
}

export function parseUnreadCount(value: unknown): number {
  return parseWithContract(unreadCountSchema, value).unreadCount;
}
