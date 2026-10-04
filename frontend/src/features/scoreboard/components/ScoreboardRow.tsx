import { Avatar } from '@/components/ui/avatar';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { Card } from '@/components/ui/card';
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
    <Card
      as="li"
      className={cn('flex items-center gap-3 p-3', isCurrentUser && 'ring-primary ring-2')}
    >
      <span className="text-muted-foreground w-6 shrink-0 text-center font-bold tabular-nums">
        {position}
      </span>
      <ProfileAvatarLink userId={entry.userId}>
        <Avatar
          name={entry.username}
          profileImage={entry.profileImage}
          className="size-8 text-xs"
        />
      </ProfileAvatarLink>
      <div className="min-w-0 flex-1">
        <p className="text-foreground truncate text-sm font-semibold">
          <ProfileLink userId={entry.userId}>{entry.username}</ProfileLink>
          {isCurrentUser ? <span className="text-muted-foreground font-normal"> (you)</span> : null}
        </p>
        <div className="bg-muted mt-1.5 h-1 w-full overflow-hidden rounded-full">
          <div
            className="bg-primary h-full rounded-full transition-[width] duration-500 ease-out"
            style={{ width: `${fillPercent}%` }}
          />
        </div>
      </div>
      <span className="text-primary shrink-0 text-sm font-bold tabular-nums">
        {entry.annualScore} Circle Points
      </span>
    </Card>
  );
}
