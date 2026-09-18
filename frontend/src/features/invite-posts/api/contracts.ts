import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';
import type { ApiSchemas } from '@/types/api-types';

export type CreateInvitePostRequest = ApiSchemas['CreateInvitePostRequest'];
export type InviteType = CreateInvitePostRequest['inviteType'];
export type LocationScope = CreateInvitePostRequest['locationScope'];
/** GLOBAL is a distinct feed, not a filter within it — the local feed only ever takes these. */
export type LocalFeedScope = Exclude<LocationScope, 'GLOBAL'>;

export const SCOPE_LABELS: Record<LocationScope, string> = {
  CITY: 'City',
  STATE_REGION: 'State',
  COUNTRY: 'Country',
  GLOBAL: 'Global',
};

const invitePostSchema = z
  .object({
    id: z.string().min(1),
    posterId: z.string().min(1),
    posterUsername: z.string().min(1),
    content: z.string().min(1),
    inviteType: z.enum(['SINGLE', 'GROUP']),
    totalCapacity: z.number().int().positive(),
    acceptedCount: z.number().int().nonnegative(),
    invitesLeft: z.number().int().nonnegative(),
    locationScope: z.enum(['CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL']),
    city: z.string().min(1),
    stateRegion: z.string().nullish(),
    country: z.string().min(1),
    status: z.enum(['ACTIVE', 'CLOSED']),
    expiresAt: z.string().min(1),
    createdAt: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

export type InvitePost = z.infer<typeof invitePostSchema>;

const invitePostListSchema = z.array(invitePostSchema);

export function parseInvitePost(value: unknown): InvitePost {
  return parseWithContract(invitePostSchema, value);
}

export function parseInvitePostList(value: unknown): InvitePost[] {
  return parseWithContract(invitePostListSchema, value);
}
