import { z } from 'zod';
import { parseWithContract } from '@/lib/api/contracts';

const sessionSchema = z
  .object({
    id: z.string().min(1),
    userAgent: z.string().nullable(),
    current: z.boolean(),
    createdAt: z.string().min(1),
    lastUsedAt: z.string().min(1),
    inactiveAt: z.string().min(1),
    expiresAt: z.string().min(1),
  })
  .passthrough();

export type Session = z.infer<typeof sessionSchema>;

const sessionsResponseSchema = z.object({
  sessions: z.array(sessionSchema),
});

export function parseSessions(value: unknown): Session[] {
  return parseWithContract(sessionsResponseSchema, value).sessions;
}
