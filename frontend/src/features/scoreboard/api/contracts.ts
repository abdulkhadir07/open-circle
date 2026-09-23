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

const scoreSummarySchema = z
  .object({
    userId: z.string().min(1),
    seasonYear: z.number(),
    annualScore: z.number(),
    lifetimeScore: z.number(),
  })
  .passthrough();

export type ScoreSummary = z.infer<typeof scoreSummarySchema>;

const scoreboardEntrySchema = z
  .object({
    rank: z.number(),
    userId: z.string().min(1),
    username: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
    annualScore: z.number(),
    averageRating: z.number().nullable(),
    currentYearDistinctRaterCount: z.number(),
  })
  .passthrough();

export type ScoreboardEntry = z.infer<typeof scoreboardEntrySchema>;

const scoreboardSchema = z
  .object({
    seasonYear: z.number(),
    entries: z.array(scoreboardEntrySchema),
  })
  .passthrough();

export type Scoreboard = z.infer<typeof scoreboardSchema>;

export function parseScoreSummary(value: unknown): ScoreSummary {
  return parseWithContract(scoreSummarySchema, value);
}

export function parseScoreboard(value: unknown): Scoreboard {
  return parseWithContract(scoreboardSchema, value);
}
