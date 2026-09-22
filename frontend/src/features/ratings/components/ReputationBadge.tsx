import { useReputation } from '../hooks/useReputation';

export function ReputationBadge({ userId }: { userId: string | undefined }) {
  const reputation = useReputation(userId);

  if (!reputation.data || reputation.data.totalRatingsReceived === 0) return null;

  const { averageRating, totalRatingsReceived } = reputation.data;
  const average = averageRating?.toFixed(1) ?? '—';

  return (
    <span
      title={`${average}/5 average · ${totalRatingsReceived} ${totalRatingsReceived === 1 ? 'rating' : 'ratings'}`}
      className="text-muted-foreground inline-flex items-center gap-0.5 text-sm tabular-nums"
    >
      {average}/5
    </span>
  );
}
