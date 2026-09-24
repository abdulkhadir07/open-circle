import { Trophy } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import type { ProfileAward } from '../api/contracts';

function formatAwardDate(iso: string): string {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
}

export function ProfileAwardsList({ awards }: { awards: ProfileAward[] }) {
  const reduceMotion = useReducedMotion();

  if (awards.length === 0) {
    return (
      <div className="border-border flex flex-col items-center gap-2 rounded-xl border border-dashed py-8 text-center">
        <Trophy aria-hidden="true" className="text-muted-foreground size-5" />
        <p className="text-muted-foreground text-base">No awards yet.</p>
      </div>
    );
  }

  return (
    <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {awards.map((award, index) => (
        <motion.li
          key={award.seasonYear}
          initial={reduceMotion ? false : { opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: reduceMotion ? 0 : 0.3,
            delay: reduceMotion ? 0 : index * 0.06,
            ease: 'easeOut',
          }}
          className="flex items-center gap-3 rounded-xl border border-amber-500/25 bg-amber-500/5 p-4"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-amber-500/20 text-amber-600">
            <Trophy aria-hidden="true" className="size-5" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-foreground truncate text-base font-semibold">{award.name}</p>
            <p className="text-muted-foreground text-sm">{formatAwardDate(award.awardedAt)}</p>
          </div>
          <span className="text-foreground shrink-0 text-sm font-semibold tabular-nums">
            {award.finalScore} Circle Points
          </span>
        </motion.li>
      ))}
    </ul>
  );
}
