import { z } from 'zod';

/**
 * Mirrors the backend's exact constraints (CreateInvitePostRequest,
 * InvitePost, InvitePostService): content is required and capped at 500
 * chars; SINGLE invites are always capacity 1 (not collected from the
 * user); GROUP invites need a capacity of at least 2, with no upper bound
 * on either side, since the backend doesn't have one either.
 *
 * `stateRegionAvailable` mirrors InvitePostService.validateLocationScope —
 * STATE_REGION is only a valid choice when the poster's verified location
 * actually has one.
 */
export function createInvitePostSchema(stateRegionAvailable: boolean) {
  return z
    .object({
      content: z.string().trim().min(1, 'Content is required').max(500, 'Content is too long'),
      inviteType: z.enum(['SINGLE', 'GROUP']),
      // Kept as the raw string a native number input actually produces;
      // parsed to a number only where it's checked, below and at submit time.
      totalCapacity: z.string(),
      locationScope: z.enum(['CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL']),
    })
    .refine(
      (values) => {
        if (values.inviteType !== 'GROUP') return true;
        const capacity = Number(values.totalCapacity);
        return Number.isInteger(capacity) && capacity >= 2;
      },
      { message: 'Group invites need a capacity of at least 2', path: ['totalCapacity'] },
    )
    .refine((values) => values.locationScope !== 'STATE_REGION' || stateRegionAvailable, {
      message: "State/region isn't available for your verified location",
      path: ['locationScope'],
    });
}

export type CreateInvitePostFormValues = z.infer<ReturnType<typeof createInvitePostSchema>>;
