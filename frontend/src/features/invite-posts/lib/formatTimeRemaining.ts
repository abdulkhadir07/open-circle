const HOUR_MS = 60 * 60 * 1000;
const MINUTE_MS = 60 * 1000;

/**
 * Purely a display concern — the backend already guarantees an expired post
 * can never be returned by a feed query (`expiresAt > now` is enforced at
 * the database level), so this never needs to hide a card itself.
 */
export function formatTimeRemaining(msRemaining: number): string {
  if (msRemaining <= 0) return 'Expired';

  const hours = Math.floor(msRemaining / HOUR_MS);
  if (hours >= 1) return `${hours}h left`;

  const minutes = Math.floor(msRemaining / MINUTE_MS);
  if (minutes >= 1) return `${minutes}m left`;

  return 'Less than a minute left';
}
