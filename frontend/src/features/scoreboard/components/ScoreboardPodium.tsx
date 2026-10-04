import { Crown } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import type { ScoreboardEntry } from '../api/contracts';

type Place = 1 | 2 | 3;

function PodiumSlot({
  entry,
  place,
  isCurrentUser,
}: {
  entry: ScoreboardEntry;
  place: Place;
  isCurrentUser: boolean;
}) {
  return (
    <Card
      className={cn(
        'animate-fade-up flex flex-col items-center gap-2 p-4 text-center',
        place === 1 && 'border-primary pb-8',
        isCurrentUser && 'ring-primary ring-2',
      )}
    >
      {place === 1 ? (
        <Crown aria-hidden="true" className="text-primary size-5" />
      ) : (
        <span className="text-muted-foreground text-sm font-bold">#{place}</span>
      )}
      <ProfileAvatarLink userId={entry.userId}>
        <Avatar
          name={entry.username}
          profileImage={entry.profileImage}
          className={place === 1 ? 'size-14 text-xl' : 'size-11 text-base'}
        />
      </ProfileAvatarLink>
      <p className="max-w-full truncate text-sm font-semibold">
        <ProfileLink userId={entry.userId}>{entry.username}</ProfileLink>
        {isCurrentUser ? <span className="text-muted-foreground font-normal"> (you)</span> : null}
      </p>
      <p className="text-primary text-sm font-bold tabular-nums">
        {entry.annualScore} Circle Points
      </p>
    </Card>
  );
}

type ScoreboardPodiumProps = {
  entries: ScoreboardEntry[];
  currentUserId: string | undefined;
};

/** Renders the top 3 entries as a podium (2nd, 1st, 3rd) — the rest of the list is handled elsewhere. */
export function ScoreboardPodium({ entries, currentUserId }: ScoreboardPodiumProps) {
  const [first, second, third] = entries;
  if (!first) return null;

  const slots: { entry: ScoreboardEntry | undefined; place: Place }[] = [
    { entry: second, place: 2 },
    { entry: first, place: 1 },
    { entry: third, place: 3 },
  ];

  return (
    <div className="mb-4 grid grid-cols-3 items-end gap-3">
      {slots.map(({ entry, place }) =>
        entry ? (
          <PodiumSlot
            key={entry.userId}
            entry={entry}
            place={place}
            isCurrentUser={entry.userId === currentUserId}
          />
        ) : (
          <div key={place} />
        ),
      )}
    </div>
  );
}
