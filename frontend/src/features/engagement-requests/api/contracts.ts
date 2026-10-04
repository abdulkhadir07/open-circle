import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

export const ENGAGEMENT_REQUEST_STATUSES = [
  'PENDING',
  'ACCEPTED',
  'DECLINED',
  'HELD',
  'WITHDRAWN',
] as const;

export type EngagementRequestStatus = (typeof ENGAGEMENT_REQUEST_STATUSES)[number];

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const engagementInvitePostSummarySchema = z
  .object({
    id: z.string().min(1),
    // Absent on responses from a backend that predates it.
    posterId: z.string().nullish(),
    content: z.string().min(1),
    posterUsername: z.string().min(1),
    city: z.string().min(1),
    stateRegion: z.string().nullish(),
    country: z.string().min(1),
  })
  .passthrough();

export type EngagementInvitePostSummary = z.infer<typeof engagementInvitePostSummarySchema>;

const engagementRequestSchema = z
  .object({
    id: z.string().min(1),
    invitePostId: z.string().min(1),
    invitePost: engagementInvitePostSummarySchema,
    requesterId: z.string().min(1),
    requesterUsername: z.string().min(1),
    requesterProfileImage: profileImageSchema.nullish(),
    status: z.enum(ENGAGEMENT_REQUEST_STATUSES),
    expiresAt: z.string().min(1),
    respondedAt: z.string().nullish(),
    withdrawnAt: z.string().nullish(),
    createdAt: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

export type EngagementRequest = z.infer<typeof engagementRequestSchema>;

const engagementRequestListSchema = z.array(engagementRequestSchema);

export function parseEngagementRequest(value: unknown): EngagementRequest {
  return parseWithContract(engagementRequestSchema, value);
}

export function parseEngagementRequestList(value: unknown): EngagementRequest[] {
  return parseWithContract(engagementRequestListSchema, value);
}
