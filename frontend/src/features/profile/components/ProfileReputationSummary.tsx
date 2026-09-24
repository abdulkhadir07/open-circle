import { formatAverageRating } from '@/lib/formatRating';
import type { ProfileReputation } from '../api/contracts';

export function ProfileReputationSummary({ reputation }: { reputation: ProfileReputation }) {
  if (reputation.totalRatingsReceived === 0) {
    return (
      <div className="border-border flex flex-col items-center gap-1 rounded-xl border border-dashed py-8 text-center">
        <p className="text-muted-foreground text-base">No ratings yet.</p>
      </div>
    );
  }

  const average = formatAverageRating(reputation.averageRating);

  return (
    <div className="border-border bg-card flex items-center justify-between gap-4 rounded-xl border p-4">
      <div>
        <p className="text-foreground text-2xl font-semibold tabular-nums">{average}/5</p>
        <p className="text-muted-foreground text-sm">Average rating</p>
      </div>
      <div className="text-right">
        <p className="text-foreground text-base font-semibold tabular-nums">
          {reputation.totalRatingsReceived}
        </p>
        <p className="text-muted-foreground text-sm">
          {reputation.totalRatingsReceived === 1 ? 'Rating' : 'Ratings'} from{' '}
          {reputation.distinctRaterCount}{' '}
          {reputation.distinctRaterCount === 1 ? 'person' : 'people'}
        </p>
      </div>
    </div>
  );
}
