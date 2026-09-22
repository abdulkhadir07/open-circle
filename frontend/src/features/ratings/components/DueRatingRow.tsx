import { useEffect, useState } from 'react';
import type { DueRating } from '../api/contracts';
import { formatDueCountdown } from '../lib/formatDueCountdown';
import { copyForTrigger } from '../lib/ratingTriggerCopy';
import { RateEngagementDialog } from './RateEngagementDialog';

const REFRESH_INTERVAL_MS = 60_000;

export function DueRatingRow({ dueRating }: { dueRating: DueRating }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, []);

  const msRemaining = new Date(dueRating.dueAt).getTime() - now;

  return (
    <li className="border-border bg-card flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4">
      <div className="flex min-w-0 items-center gap-3">
        <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
          {dueRating.otherUsername[0]?.toUpperCase()}
        </span>
        <div className="min-w-0">
          <p className="text-foreground text-base font-semibold">{dueRating.otherUsername}</p>
          <p className="text-muted-foreground mt-0.5 text-sm">
            {copyForTrigger(dueRating.trigger)}
          </p>
          <p className="text-muted-foreground mt-1 text-sm tabular-nums">
            {formatDueCountdown(msRemaining)}
          </p>
        </div>
      </div>
      <RateEngagementDialog
        engagementId={dueRating.engagementId}
        otherUsername={dueRating.otherUsername}
      />
    </li>
  );
}
