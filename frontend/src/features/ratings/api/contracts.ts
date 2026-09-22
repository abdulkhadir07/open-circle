import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

export const RATING_TRIGGERS = ['PARTICIPANT_EXIT', 'CHAT_INACTIVITY', 'MAX_DURATION'] as const;

export type RatingTrigger = (typeof RATING_TRIGGERS)[number];

const profileImageSchema = z
  .object({
    id: z.string().min(1),
    url: z.string().min(1),
    urlExpiresAt: z.string().min(1),
    contentType: z.string().min(1),
    updatedAt: z.string().min(1),
  })
  .passthrough();

const dueRatingSchema = z
  .object({
    obligationId: z.string().min(1),
    engagementId: z.string().min(1),
    otherUserId: z.string().min(1),
    otherUsername: z.string().min(1),
    otherUserProfileImage: profileImageSchema.nullish(),
    trigger: z.enum(RATING_TRIGGERS),
    requiredAt: z.string().min(1),
    dueAt: z.string().min(1),
  })
  .passthrough();

export type DueRating = z.infer<typeof dueRatingSchema>;

const dueRatingListSchema = z.array(dueRatingSchema);

const ratingSubmissionSchema = z
  .object({
    id: z.string().min(1),
    engagementId: z.string().min(1),
    ratedUserId: z.string().min(1),
    score: z.number(),
    submittedAt: z.string().min(1),
    revealed: z.boolean(),
    revealedAt: z.string().nullish(),
  })
  .passthrough();

export type RatingSubmission = z.infer<typeof ratingSubmissionSchema>;

const receivedRatingSchema = z
  .object({
    id: z.string().min(1),
    engagementId: z.string().min(1),
    raterUserId: z.string().min(1),
    raterUsername: z.string().min(1),
    raterProfileImage: profileImageSchema.nullish(),
    score: z.number(),
    revealedAt: z.string().min(1),
  })
  .passthrough();

export type ReceivedRating = z.infer<typeof receivedRatingSchema>;

const receivedRatingsPageSchema = z
  .object({
    ratings: z.array(receivedRatingSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type ReceivedRatingsPage = z.infer<typeof receivedRatingsPageSchema>;

const reputationSchema = z
  .object({
    userId: z.string().min(1),
    username: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
    averageRating: z.number().nullable(),
    totalRatingsReceived: z.number(),
    distinctRaterCount: z.number(),
  })
  .passthrough();

export type Reputation = z.infer<typeof reputationSchema>;

export function parseDueRatings(value: unknown): DueRating[] {
  return parseWithContract(dueRatingListSchema, value);
}

export function parseRatingSubmission(value: unknown): RatingSubmission {
  return parseWithContract(ratingSubmissionSchema, value);
}

export function parseReceivedRatingsPage(value: unknown): ReceivedRatingsPage {
  return parseWithContract(receivedRatingsPageSchema, value);
}

export function parseReputation(value: unknown): Reputation {
  return parseWithContract(reputationSchema, value);
}
