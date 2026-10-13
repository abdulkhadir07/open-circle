import { z } from 'zod';

export const deleteAccountSchema = z.object({
  currentPassword: z
    .string()
    .min(1, 'Current password is required')
    .max(72, 'Current password must be 72 characters or fewer'),
  confirmation: z.string().refine((value): boolean => {
    return value === 'DELETE';
  }, 'Type DELETE to confirm account deletion'),
});

export type DeleteAccountFormValues = z.infer<typeof deleteAccountSchema>;
