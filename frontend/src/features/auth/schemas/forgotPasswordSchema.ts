import { z } from 'zod';

export const forgotPasswordSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').max(160).email('Enter a valid email'),
});

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
