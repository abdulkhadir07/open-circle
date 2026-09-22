const HOUR_MS = 60 * 60 * 1000;
const MINUTE_MS = 60 * 1000;

/**
 * Purely a display concern — a due rating past its window just stops being
 * returned by the backend on the next fetch, so this never needs to hide a
 * row itself.
 */
export function formatDueCountdown(msRemaining: number): string {
  if (msRemaining <= 0) return 'Expired';

  const hours = Math.floor(msRemaining / HOUR_MS);
  if (hours >= 1) return `${hours}h left to rate`;

  const minutes = Math.floor(msRemaining / MINUTE_MS);
  if (minutes >= 1) return `${minutes}m left to rate`;

  return 'Less than a minute left to rate';
}
