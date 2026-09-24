import { ArrowLeft, LoaderCircle } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { Link, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { formatAverageRating } from '@/lib/formatRating';
import { cn } from '@/lib/utils';
import { DueRatingRow } from '../components/DueRatingRow';
import { ReceivedRatingRow } from '../components/ReceivedRatingRow';
import { useDueRatings } from '../hooks/useDueRatings';
import { useReceivedRatings } from '../hooks/useReceivedRatings';
import { useReputation } from '../hooks/useReputation';

const TAB_OPTIONS = [
  { value: 'due', label: 'Due' },
  { value: 'received', label: 'Received' },
] as const;

type Tab = (typeof TAB_OPTIONS)[number]['value'];

export function RatingsPage() {
  const reduceMotion = useReducedMotion();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab: Tab = searchParams.get('tab') === 'received' ? 'received' : 'due';

  const currentUser = useCurrentUser();
  const reputation = useReputation(currentUser.data?.id);
  const dueRatings = useDueRatings();
  const receivedRatings = useReceivedRatings();

  function selectTab(next: Tab) {
    setSearchParams(next === 'due' ? {} : { tab: next }, { replace: true });
  }

  const received = receivedRatings.data?.pages.flatMap((page) => page.ratings) ?? [];

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back
      </Link>

      <p className="text-primary mt-4 text-sm font-semibold">Your circle</p>
      <h1 className="text-foreground text-3xl font-semibold">Ratings</h1>

      {reputation.data && reputation.data.totalRatingsReceived > 0 ? (
        <div className="border-border bg-card mt-4 flex items-center gap-2 rounded-xl border p-4">
          <p className="text-foreground text-base">
            <span className="font-semibold">
              {formatAverageRating(reputation.data.averageRating)}/5
            </span>{' '}
            average from {reputation.data.distinctRaterCount}{' '}
            {reputation.data.distinctRaterCount === 1 ? 'person' : 'people'} ·{' '}
            {reputation.data.totalRatingsReceived}{' '}
            {reputation.data.totalRatingsReceived === 1 ? 'rating' : 'ratings'} total
          </p>
        </div>
      ) : null}

      <div
        role="radiogroup"
        aria-label="Ratings"
        className="border-border mt-6 flex items-baseline gap-7 border-b"
      >
        {TAB_OPTIONS.map((option) => {
          const selected = option.value === tab;
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => selectTab(option.value)}
              className={cn(
                'relative pb-3 text-xl font-semibold tracking-tight transition-colors',
                selected
                  ? 'text-foreground'
                  : 'text-muted-foreground/60 hover:text-muted-foreground',
              )}
            >
              {option.label}
              {selected ? (
                <motion.span
                  layoutId="ratings-tab-underline"
                  className="bg-primary absolute inset-x-0 bottom-0 h-[2.5px] rounded-full"
                  transition={
                    reduceMotion ? { duration: 0 } : { duration: 0.35, ease: [0.22, 1, 0.36, 1] }
                  }
                />
              ) : null}
            </button>
          );
        })}
      </div>

      <div className="mt-6">
        {tab === 'due' ? (
          dueRatings.isLoading ? (
            <LoaderCircle
              aria-hidden="true"
              className="text-muted-foreground mx-auto block size-5 animate-spin"
            />
          ) : dueRatings.isError ? (
            <p role="alert" className="text-destructive text-base">
              {dueRatings.error instanceof Error
                ? dueRatings.error.message
                : 'Unable to load ratings due.'}
            </p>
          ) : !dueRatings.data || dueRatings.data.length === 0 ? (
            <p className="text-muted-foreground text-base">Nothing to rate right now.</p>
          ) : (
            <ul className="space-y-3">
              {dueRatings.data.map((due) => (
                <DueRatingRow key={due.obligationId} dueRating={due} />
              ))}
            </ul>
          )
        ) : receivedRatings.isLoading ? (
          <LoaderCircle
            aria-hidden="true"
            className="text-muted-foreground mx-auto block size-5 animate-spin"
          />
        ) : receivedRatings.isError ? (
          <p role="alert" className="text-destructive text-base">
            {receivedRatings.error instanceof Error
              ? receivedRatings.error.message
              : 'Unable to load your ratings.'}
          </p>
        ) : received.length === 0 ? (
          <p className="text-muted-foreground text-base">No ratings yet.</p>
        ) : (
          <div>
            <ul className="space-y-3">
              {received.map((rating) => (
                <ReceivedRatingRow key={rating.id} rating={rating} />
              ))}
            </ul>
            {receivedRatings.hasNextPage ? (
              <div className="mt-4 flex justify-center">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => void receivedRatings.fetchNextPage()}
                  disabled={receivedRatings.isFetchingNextPage}
                >
                  {receivedRatings.isFetchingNextPage ? (
                    <LoaderCircle aria-hidden="true" className="animate-spin" />
                  ) : null}
                  Load more
                </Button>
              </div>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );
}
