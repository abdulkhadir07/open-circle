import { Crown } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { cn } from '@/lib/utils';
import type { ScoreboardEntry } from '../api/contracts';

type Place = 1 | 2 | 3;

const MEDAL_STYLES: Record<Place, { ring: string; avatar: string; badge: string }> = {
  1: {
    ring: 'ring-2 ring-amber-500',
    avatar: 'bg-amber-500/25 text-amber-800',
    badge: 'bg-amber-500 text-amber-50',
  },
  2: {
    ring: 'ring-2 ring-slate-500',
    avatar: 'bg-slate-500/20 text-slate-700',
    badge: 'bg-slate-500 text-slate-50',
  },
  3: {
    ring: 'ring-2 ring-orange-700',
    avatar: 'bg-orange-700/20 text-orange-900',
    badge: 'bg-orange-700 text-orange-50',
  },
};

function PodiumSlot({
  entry,
  place,
  isCurrentUser,
}: {
  entry: ScoreboardEntry;
  place: Place;
  isCurrentUser: boolean;
}) {
  const styles = MEDAL_STYLES[place];

  return (
    <div className="flex flex-1 flex-col items-center gap-2">
      <div className="relative">
        <span
          className={cn(
            'flex items-center justify-center rounded-full font-bold',
            styles.ring,
            styles.avatar,
            place === 1 ? 'size-16 text-xl' : 'size-12 text-base',
          )}
        >
          {entry.username[0]?.toUpperCase()}
        </span>
        {place === 1 ? (
          <Crown
            aria-hidden="true"
            className="absolute -top-4 left-1/2 size-5 -translate-x-1/2 fill-amber-500 text-amber-600"
          />
        ) : (
          <span
            className={cn(
              'absolute -right-1 -bottom-1 flex size-5 items-center justify-center rounded-full text-xs font-bold',
              styles.badge,
            )}
          >
            {place}
          </span>
        )}
      </div>
      <p
        className={cn(
          'max-w-full truncate text-center text-sm font-semibold',
          isCurrentUser ? 'text-primary' : 'text-foreground',
        )}
      >
        {entry.username}
        {isCurrentUser ? <span className="text-muted-foreground font-normal"> (you)</span> : null}
      </p>
      <p className="text-muted-foreground text-xs font-semibold tabular-nums">
        {entry.annualScore} Circle Points
      </p>
    </div>
  );
}

type ScoreboardPodiumProps = {
  entries: ScoreboardEntry[];
  currentUserId: string | undefined;
};

/** Renders the top 3 entries as a podium — the rest of the list is handled elsewhere. */
export function ScoreboardPodium({ entries, currentUserId }: ScoreboardPodiumProps) {
  const reduceMotion = useReducedMotion();
  const [first, second, third] = entries;
  if (!first) return null;

  const slots: { entry: ScoreboardEntry; place: Place }[] = [
    ...(second ? [{ entry: second, place: 2 as const }] : []),
    { entry: first, place: 1 as const },
    ...(third ? [{ entry: third, place: 3 as const }] : []),
  ];

  return (
    <div className="flex items-start justify-center gap-4 pt-6 pb-2">
      {slots.map(({ entry, place }, index) => (
        <motion.div
          key={entry.userId}
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: reduceMotion ? 0 : 0.3,
            delay: reduceMotion ? 0 : index * 0.08,
            ease: 'easeOut',
          }}
          className="flex flex-1"
        >
          <PodiumSlot entry={entry} place={place} isCurrentUser={entry.userId === currentUserId} />
        </motion.div>
      ))}
    </div>
  );
}
