import { cn } from '@/lib/utils';
import type { ScoreboardEntry } from '../api/contracts';

type ScoreboardRowProps = {
  entry: ScoreboardEntry;
  /** This row's position in the displayed list (4, 5, ...) — not the raw, tie-affected API rank. */
  position: number;
  isCurrentUser?: boolean;
  /** This entry's score relative to the top score on the board, 0-1 — drives the fill bar. */
  scoreFraction: number;
};

export function ScoreboardRow({
  entry,
  position,
  isCurrentUser = false,
  scoreFraction,
}: ScoreboardRowProps) {
  const fillPercent = Math.round(Math.min(1, Math.max(0, scoreFraction)) * 100);

  return (
    <li className={cn('flex items-center gap-3 px-4 py-3', isCurrentUser && 'bg-primary/5')}>
      <span className="text-muted-foreground w-5 shrink-0 text-center text-sm font-semibold tabular-nums">
        {position}
      </span>
      <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
        {entry.username[0]?.toUpperCase()}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-foreground truncate text-sm font-semibold">
          {entry.username}
          {isCurrentUser ? <span className="text-muted-foreground font-normal"> (you)</span> : null}
        </p>
        <div className="bg-muted mt-1.5 h-1 w-full overflow-hidden rounded-full">
          <div
            className="bg-primary h-full rounded-full transition-[width] duration-500 ease-out"
            style={{ width: `${fillPercent}%` }}
          />
        </div>
      </div>
      <span className="text-foreground shrink-0 text-sm font-semibold tabular-nums">
        {entry.annualScore} Circle Points
      </span>
    </li>
  );
}
