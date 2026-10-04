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
    // The viewer's own season rank at any position; null/absent when unranked.
    rank: z.number().nullish(),
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

const annualAwardWinnerSchema = z
  .object({
    userId: z.string().min(1),
    username: z.string().min(1),
    profileImage: profileImageSchema.nullish(),
    finalScore: z.number(),
  })
  .passthrough();

export type AnnualAwardWinner = z.infer<typeof annualAwardWinnerSchema>;

const annualAwardSchema = z
  .object({
    seasonYear: z.number(),
    name: z.string().min(1),
    finalizedAt: z.string().min(1),
    winners: z.array(annualAwardWinnerSchema),
  })
  .passthrough();

export type AnnualAward = z.infer<typeof annualAwardSchema>;

export function parseScoreSummary(value: unknown): ScoreSummary {
  return parseWithContract(scoreSummarySchema, value);
}

export function parseScoreboard(value: unknown): Scoreboard {
  return parseWithContract(scoreboardSchema, value);
}

export function parseAnnualAward(value: unknown): AnnualAward {
  return parseWithContract(annualAwardSchema, value);
}
