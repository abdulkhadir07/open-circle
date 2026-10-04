import { z } from 'zod';
import { MAX_TAGS } from '../lib/tags';

/**
 * Mirrors the backend's constraints (CreateInvitePostRequest, InvitePost): content is required
 * and capped at 500 chars; SINGLE invites are always capacity 1 (not collected from the user);
 * GROUP invites need a capacity of at least 2, with no upper bound on either side, since the
 * backend doesn't have one either. Every invite goes to the poster's campus, so there is no
 * audience to choose.
 */
export const createInvitePostSchema = z
  .object({
    content: z.string().trim().min(1, 'Content is required').max(500, 'Content is too long'),
    inviteType: z.enum(['SINGLE', 'GROUP']),
    // Kept as the raw string a native number input actually produces;
    // parsed to a number only where it's checked, below and at submit time.
    totalCapacity: z.string(),
    tags: z.array(z.string()).max(MAX_TAGS, `Pick up to ${MAX_TAGS} topics`),
  })
  .refine(
    (values) => {
      if (values.inviteType !== 'GROUP') return true;
      const capacity = Number(values.totalCapacity);
      return Number.isInteger(capacity) && capacity >= 2;
    },
    { message: 'Group invites need a capacity of at least 2', path: ['totalCapacity'] },
  );

export type CreateInvitePostFormValues = z.infer<typeof createInvitePostSchema>;
