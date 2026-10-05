import { AnimatedError } from '@/features/auth/components/AnimatedError';
import { isContentBlockedError } from '@/lib/api/errors';
import { SafetyBlockedNotice } from './SafetyBlockedNotice';

/** The error under a "post something" box: a friendly notice when blocked, plain text otherwise. */
export function ContentError({ error, fallback }: { error: unknown; fallback: string }) {
  if (!error) return <AnimatedError />;

  if (isContentBlockedError(error)) {
    return <SafetyBlockedNotice message={error.message} />;
  }

  return <AnimatedError message={error instanceof Error ? error.message : fallback} />;
}
