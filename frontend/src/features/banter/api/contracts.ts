import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

export type BanterSort = 'new' | 'hot';

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const banterSchema = z
  .object({
    id: z.string().min(1),
    authorId: z.string().min(1),
    authorUsername: z.string().min(1),
    authorProfileImage: profileImageSchema.nullish(),
    content: z.string().min(1),
    createdAt: z.string().min(1),
    likeCount: z.number().int().nonnegative(),
    replyCount: z.number().int().nonnegative(),
    likedByMe: z.boolean(),
    mine: z.boolean(),
  })
  .passthrough();

export type Banter = z.infer<typeof banterSchema>;

const banterPageSchema = z
  .object({
    items: z.array(banterSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type BanterPage = z.infer<typeof banterPageSchema>;

const banterLikeSchema = z
  .object({
    likeCount: z.number().int().nonnegative(),
    likedByMe: z.boolean(),
  })
  .passthrough();

export type BanterLike = z.infer<typeof banterLikeSchema>;

const banterReplySchema = z
  .object({
    id: z.string().min(1),
    banterId: z.string().min(1),
    authorId: z.string().min(1),
    authorUsername: z.string().min(1),
    authorProfileImage: profileImageSchema.nullish(),
    content: z.string().min(1),
    createdAt: z.string().min(1),
    mine: z.boolean(),
  })
  .passthrough();

export type BanterReply = z.infer<typeof banterReplySchema>;

const banterReplyListSchema = z.array(banterReplySchema);

export function parseBanter(value: unknown): Banter {
  return parseWithContract(banterSchema, value);
}

export function parseBanterPage(value: unknown): BanterPage {
  return parseWithContract(banterPageSchema, value);
}

export function parseBanterLike(value: unknown): BanterLike {
  return parseWithContract(banterLikeSchema, value);
}

export function parseBanterReply(value: unknown): BanterReply {
  return parseWithContract(banterReplySchema, value);
}

export function parseBanterReplyList(value: unknown): BanterReply[] {
  return parseWithContract(banterReplyListSchema, value);
}
