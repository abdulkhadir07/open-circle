import { ArrowLeft, ChevronLeft, ChevronRight, LoaderCircle, Trophy } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/api/errors';
import { useAnnualAward } from '../hooks/useAnnualAward';

const LATEST_POSSIBLE_YEAR = new Date().getUTCFullYear() - 1;

export function HallOfFamePage() {
  const reduceMotion = useReducedMotion();
  const [year, setYear] = useState(LATEST_POSSIBLE_YEAR);
  const award = useAnnualAward(year);

  const notFinalized = award.error instanceof ApiError && award.error.status === 404;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/scoreboard"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back to Scoreboard
      </Link>

      <p className="text-primary mt-4 text-sm font-semibold">Your circle</p>
      <h1 className="text-foreground text-3xl font-semibold">Hall of Fame</h1>

      <div className="mt-6 flex items-center justify-center gap-4">
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Previous year"
          onClick={() => setYear((current) => current - 1)}
        >
          <ChevronLeft aria-hidden="true" />
        </Button>
        <span className="text-foreground w-16 text-center text-xl font-semibold tabular-nums">
          {year}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Next year"
          onClick={() => setYear((current) => current + 1)}
          disabled={year >= LATEST_POSSIBLE_YEAR}
        >
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>

      <div className="mt-6">
        {award.isLoading ? (
          <LoaderCircle
            aria-hidden="true"
            className="text-muted-foreground mx-auto block size-5 animate-spin"
          />
        ) : notFinalized ? (
          <div className="border-border flex flex-col items-center gap-2 rounded-xl border border-dashed py-8 text-center">
            <Trophy aria-hidden="true" className="text-muted-foreground size-5" />
            <p className="text-muted-foreground text-base">
              No Circle Champion has been crowned for {year} yet.
            </p>
          </div>
        ) : award.isError ? (
          <p role="alert" className="text-destructive text-center text-base">
            {award.error instanceof Error ? award.error.message : 'Unable to load this award.'}
          </p>
        ) : award.data && award.data.winners.length === 0 ? (
          <div className="border-border flex flex-col items-center gap-2 rounded-xl border border-dashed py-8 text-center">
            <Trophy aria-hidden="true" className="text-muted-foreground size-5" />
            <p className="text-muted-foreground text-base">
              No one scored enough to win Circle Champion {year}.
            </p>
          </div>
        ) : award.data ? (
          <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {award.data.winners.map((winner, index) => (
              <motion.li
                key={winner.userId}
                initial={reduceMotion ? false : { opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: reduceMotion ? 0 : 0.3,
                  delay: reduceMotion ? 0 : index * 0.06,
                  ease: 'easeOut',
                }}
                className="flex items-center gap-3 rounded-xl border border-amber-500/25 bg-amber-500/5 p-4"
              >
                <Avatar
                  name={winner.username}
                  profileImage={winner.profileImage}
                  className="size-11 bg-amber-500/20 text-base text-amber-700"
                />
                <div className="min-w-0 flex-1">
                  <p className="text-foreground truncate text-base font-semibold">
                    {winner.username}
                  </p>
                  <p className="text-muted-foreground text-sm">{award.data.name}</p>
                </div>
                <span className="text-foreground shrink-0 text-sm font-semibold tabular-nums">
                  {winner.finalScore} Circle Points
                </span>
              </motion.li>
            ))}
          </ul>
        ) : null}
      </div>
    </div>
  );
}
