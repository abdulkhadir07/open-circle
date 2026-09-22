import type { ReceivedRating } from '../api/contracts';
import { formatRelativeTime } from '../lib/formatRelativeTime';

export function ReceivedRatingRow({ rating }: { rating: ReceivedRating }) {
  return (
    <li className="border-border bg-card flex items-center justify-between gap-3 rounded-xl border p-4">
      <div className="flex min-w-0 items-center gap-3">
        <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
          {rating.raterUsername[0]?.toUpperCase()}
        </span>
        <div className="min-w-0">
          <p className="text-foreground text-base font-semibold">{rating.raterUsername}</p>
          <p className="text-muted-foreground mt-0.5 text-sm">
            {formatRelativeTime(rating.revealedAt)}
          </p>
        </div>
      </div>
      <span
        className="text-foreground shrink-0 text-base font-semibold tabular-nums"
        aria-label={`${rating.score} out of 5`}
      >
        {rating.score}/5
      </span>
    </li>
  );
}
