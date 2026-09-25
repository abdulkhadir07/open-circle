import { z } from 'zod';

export const resetCodeSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').max(160).email('Enter a valid email'),
  code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit reset code'),
});

export type ResetCodeFormValues = z.infer<typeof resetCodeSchema>;
