import { LoaderCircle, MessageSquareText } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { PageHeader } from '@/components/ui/page-header';
import { Tabs } from '@/components/ui/tabs';
import { BanterCard } from '../components/BanterCard';
import { BanterComposer } from '../components/BanterComposer';
import { useBanterFeed } from '../hooks/useBanterFeed';

const TAB_ITEMS = [
  { key: 'new', to: '/banter', label: 'New' },
  { key: 'hot', to: '/banter?sort=hot', label: 'Hot' },
];

export function BanterPage() {
  const [searchParams] = useSearchParams();
  const sort = searchParams.get('sort') === 'hot' ? 'hot' : 'new';
  const feed = useBanterFeed(sort);
  const items = feed.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div>
      <PageHeader
        title="Banter"
        sub="Say whatever's on your mind. Hot takes, class rants, random thoughts. All fair game."
      />

      <BanterComposer />

      <Tabs items={TAB_ITEMS} active={sort} />

      {feed.isLoading ? (
        <LoaderCircle
          aria-hidden="true"
          className="text-muted-foreground mx-auto block size-5 animate-spin"
        />
      ) : feed.isError ? (
        <p role="alert" className="text-destructive text-base">
          {feed.error instanceof Error ? feed.error.message : 'Unable to load banter.'}
        </p>
      ) : items.length === 0 ? (
        <EmptyState icon={MessageSquareText}>It's quiet in here. Say something!</EmptyState>
      ) : (
        <div className="space-y-3">
          {items.map((banter) => (
            <BanterCard key={banter.id} banter={banter} />
          ))}
          {feed.hasNextPage ? (
            <div className="flex justify-center pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => void feed.fetchNextPage()}
                disabled={feed.isFetchingNextPage}
              >
                {feed.isFetchingNextPage ? (
                  <LoaderCircle aria-hidden="true" className="animate-spin" />
                ) : null}
                Load more
              </Button>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}
