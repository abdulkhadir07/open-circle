import { Trophy } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { EmptyState } from '@/components/ui/empty-state';
import type { ProfileAward } from '../api/contracts';

function formatAwardDate(iso: string): string {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' });
}

/** Awards as a shelf of gold trophy cards. */
export function ProfileAwardsList({ awards }: { awards: ProfileAward[] }) {
  const reduceMotion = useReducedMotion();

  if (awards.length === 0) {
    return <EmptyState icon={Trophy}>No awards yet.</EmptyState>;
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
          className="to-gold/10 bg-card relative overflow-hidden rounded-2xl border border-amber-500/30 bg-gradient-to-br from-amber-500/10 p-4"
        >
          <div className="flex items-center gap-3">
            <span className="bg-gold/25 flex size-12 shrink-0 items-center justify-center rounded-full text-amber-600 dark:text-amber-400">
              <Trophy aria-hidden="true" className="size-6" />
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-base font-semibold">{award.name}</p>
              <p className="text-muted-foreground text-xs">
                Season {award.seasonYear} · {formatAwardDate(award.awardedAt)}
              </p>
            </div>
          </div>
          <p className="mt-3 text-sm font-bold tabular-nums">{award.finalScore} Circle Points</p>
        </motion.li>
      ))}
    </ul>
  );
}
