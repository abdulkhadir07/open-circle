import { z } from 'zod';

export const MAX_DISPLAY_NAME_LENGTH = 80;
export const MAX_BIO_LENGTH = 300;
export const MAX_INTERESTS = 8;
export const MAX_INTEREST_LENGTH = 30;

/**
 * Mirrors the backend's exact constraints (UserProfile.java): display name
 * is required and capped at 80 chars; bio is optional, capped at 300;
 * interests allow at most 8 entries, each 1-30 chars, unique ignoring case.
 */
export const updateProfileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Display name is required')
    .max(MAX_DISPLAY_NAME_LENGTH, 'Display name is too long'),
  bio: z.string().trim().max(MAX_BIO_LENGTH, 'Bio is too long'),
  interests: z
    .array(
      z
        .string()
        .trim()
        .min(1, 'Interests cannot be blank')
        .max(MAX_INTEREST_LENGTH, 'Interest is too long'),
    )
    .max(MAX_INTERESTS, 'A profile can have at most 8 interests')
    .refine(
      (values) => new Set(values.map((value) => value.toLowerCase())).size === values.length,
      'Interests must be unique',
    ),
});

export type UpdateProfileFormValues = z.infer<typeof updateProfileSchema>;
