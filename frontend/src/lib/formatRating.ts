export function formatAverageRating(averageRating: number | null | undefined): string {
  if (averageRating == null) return '—';
  return Number.isInteger(averageRating) ? String(averageRating) : averageRating.toFixed(1);
}
