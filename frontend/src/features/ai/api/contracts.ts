import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

const inviteDraftSchema = z
  .object({
    content: z.string(),
    inviteType: z.enum(['SINGLE', 'GROUP']),
    totalCapacity: z.number().int().nullish(),
    tags: z.array(z.string()),
    aiGenerated: z.boolean(),
  })
  .passthrough();

export type InviteDraft = z.infer<typeof inviteDraftSchema>;

const feedInsightsSchema = z
  .object({
    digest: z.string(),
    reasons: z.array(
      z
        .object({
          invitePostId: z.string().min(1),
          reason: z.string().min(1),
        })
        .passthrough(),
    ),
    aiGenerated: z.boolean(),
  })
  .passthrough();

export type FeedInsights = z.infer<typeof feedInsightsSchema>;

export function parseInviteDraft(value: unknown): InviteDraft {
  return parseWithContract(inviteDraftSchema, value);
}

export function parseFeedInsights(value: unknown): FeedInsights {
  return parseWithContract(feedInsightsSchema, value);
}
