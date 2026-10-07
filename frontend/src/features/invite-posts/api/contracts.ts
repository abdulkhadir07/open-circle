import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';
import type { ApiSchemas } from '@/types/api-types';

export type CreateInvitePostRequest = ApiSchemas['CreateInvitePostRequest'];
export type InviteType = CreateInvitePostRequest['inviteType'];
/** The backend still knows a legacy CAMPUS scope for old rows; it is never offered or returned. */
export type LocationScope = Exclude<CreateInvitePostRequest['locationScope'], 'CAMPUS'>;
/** GLOBAL is a distinct feed, not a filter within it — the local feed only ever takes these. */
export type LocalFeedScope = Exclude<LocationScope, 'GLOBAL'>;

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const invitePostImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    originalFilename: z.string().min(1),
    contentType: z.string().min(1),
    fileSizeBytes: z.number(),
    displayOrder: z.number().int(),
    createdAt: z.string().min(1),
  })
  .passthrough();

export type InvitePostImage = z.infer<typeof invitePostImageSchema>;

const invitePostSchema = z
  .object({
    id: z.string().min(1),
    posterId: z.string().min(1),
    posterUsername: z.string().min(1),
    posterProfileImage: profileImageSchema.nullish(),
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
    images: z.array(invitePostImageSchema),
    // Absent on responses from a backend that predates topics.
    tags: z.array(z.string()).default([]),
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

export function parseInvitePostImage(value: unknown): InvitePostImage {
  return parseWithContract(invitePostImageSchema, value);
}
