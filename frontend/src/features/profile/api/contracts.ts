import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const profileReputationSchema = z
  .object({
    averageRating: z.number().nullable(),
    totalRatingsReceived: z.number(),
    distinctRaterCount: z.number(),
  })
  .passthrough();

export type ProfileReputation = z.infer<typeof profileReputationSchema>;

const profileAwardSchema = z
  .object({
    seasonYear: z.number(),
    name: z.string().min(1),
    finalScore: z.number(),
    awardedAt: z.string().min(1),
  })
  .passthrough();

export type ProfileAward = z.infer<typeof profileAwardSchema>;

const userProfileSchema = z
  .object({
    userId: z.string().min(1),
    username: z.string().min(1),
    displayName: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
    bio: z.string().nullable(),
    interests: z.array(z.string()),
    memberSince: z.string().min(1),
    reputation: profileReputationSchema,
    awards: z.array(profileAwardSchema),
  })
  .passthrough();

export type UserProfile = z.infer<typeof userProfileSchema>;

export function parseUserProfile(value: unknown): UserProfile {
  return parseWithContract(userProfileSchema, value);
}
