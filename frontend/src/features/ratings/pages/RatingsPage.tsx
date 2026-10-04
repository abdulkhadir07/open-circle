import { LoaderCircle, Star, StarOff } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { PageHeader } from '@/components/ui/page-header';
import { Tabs } from '@/components/ui/tabs';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { formatAverageRating } from '@/lib/formatRating';
import { cn } from '@/lib/utils';
import { DueRatingRow } from '../components/DueRatingRow';
import { ReceivedRatingRow } from '../components/ReceivedRatingRow';
import { useDueRatings } from '../hooks/useDueRatings';
import { useReceivedRatings } from '../hooks/useReceivedRatings';
import { useReputation } from '../hooks/useReputation';

type Tab = 'due' | 'received';

export function RatingsPage() {
  const [searchParams] = useSearchParams();
  const tab: Tab = searchParams.get('tab') === 'received' ? 'received' : 'due';

  const currentUser = useCurrentUser();
  const reputation = useReputation(currentUser.data?.id);
  const dueRatings = useDueRatings();
  const receivedRatings = useReceivedRatings();

  const dueCount = dueRatings.data?.length ?? 0;
  const tabItems = [
    { key: 'due', to: '/ratings', label: dueCount > 0 ? `Due (${dueCount})` : 'Due' },
    { key: 'received', to: '/ratings?tab=received', label: 'Received' },
  ];

  const received = receivedRatings.data?.pages.flatMap((page) => page.ratings) ?? [];

  return (
    <div>
      <PageHeader title="Ratings" sub="How did it go? Rate the people you've met up with." />

      {reputation.data && reputation.data.totalRatingsReceived > 0 ? (
        <Card className="mb-5 flex items-center gap-6">
          <div className="text-center">
            <div className="text-primary text-4xl font-bold">
              {formatAverageRating(reputation.data.averageRating)}
            </div>
            <div className="mt-1 flex justify-center">
              {[1, 2, 3, 4, 5].map((value) => (
                <Star
                  key={value}
                  aria-hidden="true"
                  className={cn(
                    'size-4',
                    value <= Math.round(reputation.data.averageRating ?? 0)
                      ? 'fill-primary text-primary'
                      : 'text-primary/30',
                  )}
                />
              ))}
            </div>
          </div>
          <p className="text-sm">
            <span className="font-semibold">
              {formatAverageRating(reputation.data.averageRating)}/5
            </span>{' '}
            average from {reputation.data.distinctRaterCount}{' '}
            {reputation.data.distinctRaterCount === 1 ? 'person' : 'people'} ·{' '}
            {reputation.data.totalRatingsReceived}{' '}
            {reputation.data.totalRatingsReceived === 1 ? 'rating' : 'ratings'} total
          </p>
        </Card>
      ) : null}

      <Tabs items={tabItems} active={tab} />

      <div>
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
            <EmptyState icon={StarOff}>Nothing to rate right now.</EmptyState>
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
          <EmptyState icon={Star}>No ratings yet.</EmptyState>
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
