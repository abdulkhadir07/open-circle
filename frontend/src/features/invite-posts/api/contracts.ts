import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';
import type { ApiSchemas } from '@/types/api-types';

export type CreateInvitePostRequest = ApiSchemas['CreateInvitePostRequest'];
export type InviteType = CreateInvitePostRequest['inviteType'];

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
    // Every invite belongs to its poster's campus. The old location fields are only present on
    // posts made before campuses existed.
    campus: z.string().nullish(),
    locationScope: z.enum(['CAMPUS', 'CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL']).nullish(),
    city: z.string().nullish(),
    stateRegion: z.string().nullish(),
    country: z.string().nullish(),
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
