import { z } from 'zod';

export const requestEmailChangeSchema = z.object({
  newEmail: z.string().trim().min(1, 'New email is required').max(160).email('Enter a valid email'),
  currentPassword: z.string().min(1, 'Current password is required'),
});

export type RequestEmailChangeFormValues = z.infer<typeof requestEmailChangeSchema>;
