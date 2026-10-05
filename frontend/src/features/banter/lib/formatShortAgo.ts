const MINUTE_MS = 60 * 1000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;

/** Compact age for banter ("just now", "5m", "3h", "2d"), as openSFSU shows it. */
export function formatShortAgo(isoDate: string, now: number = Date.now()): string {
  const elapsed = now - new Date(isoDate).getTime();

  if (elapsed < MINUTE_MS) return 'just now';
  if (elapsed < HOUR_MS) return `${Math.floor(elapsed / MINUTE_MS)}m`;
  if (elapsed < DAY_MS) return `${Math.floor(elapsed / HOUR_MS)}h`;

  return `${Math.floor(elapsed / DAY_MS)}d`;
}
