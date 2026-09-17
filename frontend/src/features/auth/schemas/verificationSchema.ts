import { z } from 'zod';

export const verificationSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').max(160).email('Enter a valid email'),
  code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit verification code'),
});

export type VerificationFormValues = z.infer<typeof verificationSchema>;
