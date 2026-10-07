import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { LoaderCircle, Plus, Search, Sparkles, X } from 'lucide-react';
import { useMemo, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { EmptyState } from '@/components/ui/empty-state';
import { PageHeader } from '@/components/ui/page-header';
import { FeedDigestCard } from '@/features/ai/components/FeedDigestCard';
import { useFeedInsights } from '@/features/ai/hooks/useFeedInsights';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { LocationVerificationPrompt } from '@/features/location/components/LocationVerificationPrompt';
import { PlaceLabel } from '@/features/location/components/PlaceLabel';
import { FeedAudienceChips } from '@/features/invite-posts/components/FeedAudienceChips';
import { InvitePostCard } from '@/features/invite-posts/components/InvitePostCard';
import { useGlobalFeed } from '@/features/invite-posts/hooks/useGlobalFeed';
import { useLocalFeed } from '@/features/invite-posts/hooks/useLocalFeed';
import {
  feedAudienceOptions,
  placeLine,
  readStoredAudience,
  scopeNames,
  storeAudience,
  type FeedAudience,
} from '@/features/invite-posts/lib/audience';
import { cn } from '@/lib/utils';
import { useMyEngagementRequests } from '@/features/engagement-requests/hooks/useMyEngagementRequests';

const IDEAS = [
  {
    label: 'Coffee chat',
    text: 'Coffee and a chat near the park this afternoon, 1 person',
    tags: ['coffee'],
  },
  {
    label: 'Grab food',
    text: 'Anyone want to grab lunch downtown at noon? 2 people',
    tags: ['food'],
  },
  { label: 'Pickup game', text: 'Need 4 players for pickup soccer at 5pm', tags: ['games'] },
  {
    label: 'Walk and talk',
    text: 'Walk around the neighborhood after work at 6pm, 2 or 3 people',
    tags: ['walk'],
  },
];

const MAX_TAG_FILTERS = 12;

function FeedHeading({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <h2
      className={cn(
        'text-muted-foreground -mb-1 px-1 text-xs font-semibold tracking-wide uppercase',
        className,
      )}
    >
      {children}
    </h2>
  );
}

function greeting() {
  const hour = new Date().getHours();
  return hour < 12 ? 'Morning' : hour < 18 ? 'Hey' : 'Evening';
}

export function HomePage() {
  const reduceMotion = useReducedMotion();
  const currentUser = useCurrentUser();
  const [pickedAudience, setPickedAudience] = useState<FeedAudience>(
    () => readStoredAudience() ?? 'NEARBY',
  );
  const [query, setQuery] = useState('');
  const [activeTag, setActiveTag] = useState<string | null>(null);

  const locationVerified = Boolean(currentUser.data?.locationVerifiedAt);
  const audienceOptions = useMemo(
    () => feedAudienceOptions(currentUser.data ?? {}),
    [currentUser.data],
  );
  // A remembered choice that doesn't exist for this place (e.g. State, after moving somewhere
  // without states) falls back to Nearby.
  const audience = audienceOptions.some((option) => option.value === pickedAudience)
    ? pickedAudience
    : 'NEARBY';

  function pickAudience(next: FeedAudience) {
    setPickedAudience(next);
    storeAudience(next);
  }

  // These run on every render regardless of the early returns below (Rules
  // of Hooks), including while the location-verification prompt is still
  // showing — `enabled` is what actually stops them from hitting the API
  // (and caching a 403) before there's a verified location to query with.
  const localFeed = useLocalFeed(
    audience === 'NEARBY' || audience === 'GLOBAL' ? undefined : audience,
    { enabled: locationVerified && audience !== 'GLOBAL' },
  );
  const globalFeed = useGlobalFeed({ enabled: locationVerified && audience === 'GLOBAL' });
  const activeFeed = audience === 'GLOBAL' ? globalFeed : localFeed;
  // Optional extra: if the AI digest is slow or fails, Home simply shows no digest.
  const insights = useFeedInsights(locationVerified);
  const reasonsByPostId = useMemo(
    () => new Map(insights.data?.reasons.map((item) => [item.invitePostId, item.reason])),
    [insights.data],
  );
  const search = query.trim().toLowerCase();
  const visiblePosts = useMemo(
    () =>
      (activeFeed.data ?? []).filter((post) => {
        if (activeTag && !post.tags.includes(activeTag)) return false;
        return (
          !search ||
          [post.content, post.posterUsername, post.city, post.country, ...post.tags]
            .join(' ')
            .toLowerCase()
            .includes(search)
        );
      }),
    [activeFeed.data, search, activeTag],
  );
  // Topics in the loaded feed, most-used first.
  const feedTags = useMemo(() => {
    const counts = new Map<string, number>();
    for (const post of activeFeed.data ?? []) {
      for (const tag of post.tags) counts.set(tag, (counts.get(tag) ?? 0) + 1);
    }
    return [...counts.entries()]
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
      .slice(0, MAX_TAG_FILTERS)
      .map(([tag]) => tag);
  }, [activeFeed.data]);
  const myRequests = useMyEngagementRequests({ enabled: locationVerified });
  const myRequestsByPostId = useMemo(
    () => new Map(myRequests.data?.map((request) => [request.invitePostId, request])),
    [myRequests.data],
  );

  if (currentUser.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-6 animate-spin" />
      </div>
    );
  }

  if (!currentUser.data?.locationVerifiedAt) {
    return <LocationVerificationPrompt />;
  }

  const user = currentUser.data;
  const place = placeLine(user);
  const names = scopeNames(user);
  const inWords: Record<FeedAudience, string> = {
    NEARBY: 'near you',
    CITY: `in ${names.CITY}`,
    STATE_REGION: `in ${names.STATE_REGION}`,
    COUNTRY: `in ${names.COUNTRY}`,
    GLOBAL: 'worldwide',
  };
  const openCount = activeFeed.data?.length ?? 0;

  const myPosts = visiblePosts.filter((post) => post.posterId === user.id);
  const otherPosts = visiblePosts.filter((post) => post.posterId !== user.id);

  function renderPost(post: (typeof visiblePosts)[number], index: number) {
    return (
      <motion.div
        key={post.id}
        initial={reduceMotion ? false : { opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: reduceMotion ? 0 : 0.3,
          delay: reduceMotion ? 0 : index * 0.05,
          ease: 'easeOut',
        }}
      >
        <InvitePostCard
          post={post}
          isOwnPost={post.posterId === user.id}
          myRequest={myRequestsByPostId.get(post.id)}
          reason={reasonsByPostId.get(post.id)}
          onTagClick={(tag) => setActiveTag(activeTag === tag ? null : tag)}
        />
      </motion.div>
    );
  }

  return (
    <div>
      <PageHeader
        title={`${greeting()}${user.firstName ? `, ${user.firstName}` : ''}`}
        sub={
          activeFeed.isLoading
            ? `Invites ${inWords[audience]}.`
            : openCount > 0
              ? `${openCount} open invite${openCount === 1 ? '' : 's'} ${inWords[audience]} right now. Jump in.`
              : `Nothing open ${inWords[audience]} yet. What are you up to today?`
        }
      />
      <PlaceLabel place={place} />

      <div className="border-primary/30 bg-card mb-6 rounded-2xl border-2 p-4 shadow-sm">
        <Link to="/new" className="group flex items-center gap-3">
          <Avatar name={`${user.firstName} ${user.lastName}`} profileImage={user.profileImage} />
          <span className="bg-background text-muted-foreground group-hover:border-primary/50 flex-1 rounded-full border px-4 py-2.5 text-sm transition">
            What do you want to do today{user.firstName ? `, ${user.firstName}` : ''}?
          </span>
          <span className="bg-primary text-primary-foreground flex size-10 items-center justify-center rounded-full transition group-hover:scale-110">
            <Plus aria-hidden="true" className="size-5" />
          </span>
        </Link>
        <div className="mt-3 flex flex-wrap gap-1.5 pl-12">
          {IDEAS.map((idea) => (
            <Link
              key={idea.label}
              to={`/new?text=${encodeURIComponent(idea.text)}&tags=${idea.tags.join(',')}`}
              className="bg-primary/10 text-primary hover:bg-primary/20 rounded-full px-3 py-1 text-xs font-medium transition active:scale-95"
            >
              {idea.label}
            </Link>
          ))}
        </div>
      </div>

      {insights.data ? (
        <FeedDigestCard digest={insights.data.digest} aiGenerated={insights.data.aiGenerated} />
      ) : null}

      <div className="relative mb-3">
        <Search
          aria-hidden="true"
          className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
        />
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search invites, places, people..."
          aria-label="Search invites"
          className="bg-card focus:ring-primary/40 w-full rounded-xl border py-2.5 pr-9 pl-9 text-sm transition outline-none focus:ring-2"
        />
        {query ? (
          <button
            type="button"
            onClick={() => setQuery('')}
            aria-label="Clear search"
            className="text-muted-foreground hover:text-foreground absolute top-1/2 right-2 -translate-y-1/2 cursor-pointer rounded p-1"
          >
            <X aria-hidden="true" className="size-4" />
          </button>
        ) : null}
      </div>

      {feedTags.length > 0 || activeTag ? (
        <fieldset className="mb-3 flex min-w-0 flex-wrap gap-1.5">
          <legend className="sr-only">Filter by topic</legend>
          {(activeTag && !feedTags.includes(activeTag) ? [activeTag, ...feedTags] : feedTags).map(
            (tag) => (
              <button
                key={tag}
                type="button"
                aria-pressed={activeTag === tag}
                onClick={() => setActiveTag(activeTag === tag ? null : tag)}
                className={cn(
                  'cursor-pointer rounded-full border px-3 py-1 text-xs font-medium transition active:scale-95',
                  activeTag === tag
                    ? 'border-primary bg-primary text-primary-foreground'
                    : 'bg-card text-muted-foreground hover:border-primary/50 hover:text-foreground',
                )}
              >
                #{tag}
              </button>
            ),
          )}
        </fieldset>
      ) : null}

      <FeedAudienceChips options={audienceOptions} value={audience} onChange={pickAudience} />

      <AnimatePresence mode="wait" initial={false}>
        {activeFeed.isLoading ? (
          <motion.div
            key="loading"
            exit={reduceMotion ? undefined : { opacity: 0 }}
            className="flex justify-center py-10"
          >
            <LoaderCircle
              aria-hidden="true"
              className="text-muted-foreground size-5 animate-spin"
            />
          </motion.div>
        ) : activeFeed.isError ? (
          <motion.p
            key="error"
            role="alert"
            exit={reduceMotion ? undefined : { opacity: 0 }}
            className="text-destructive text-base"
          >
            {activeFeed.error instanceof Error
              ? activeFeed.error.message
              : 'Unable to load invite posts.'}
          </motion.p>
        ) : visiblePosts.length > 0 ? (
          <motion.div
            key={audience}
            exit={reduceMotion ? undefined : { opacity: 0 }}
            className="flex flex-col gap-4"
          >
            {myPosts.length > 0 ? <FeedHeading>Your open invites</FeedHeading> : null}
            {myPosts.map((post, index) => renderPost(post, index))}
            {myPosts.length > 0 && otherPosts.length > 0 ? (
              <FeedHeading className="mt-2">
                {audience === 'GLOBAL' ? 'Everywhere' : 'Nearby'}
              </FeedHeading>
            ) : null}
            {otherPosts.map((post, index) => renderPost(post, myPosts.length + index))}
          </motion.div>
        ) : (
          <motion.div key="empty" exit={reduceMotion ? undefined : { opacity: 0 }}>
            {search || activeTag ? (
              <EmptyState icon={Search}>No invites match that. Try another word.</EmptyState>
            ) : (
              <EmptyState
                icon={Sparkles}
                action={
                  <Link
                    to={`/new?audience=${audience === 'NEARBY' ? 'CITY' : audience}`}
                    className="bg-primary text-primary-foreground mt-1 rounded-full px-4 py-1.5 text-xs font-medium transition hover:opacity-90 active:scale-95"
                  >
                    {audience === 'NEARBY'
                      ? 'Start an invite'
                      : audience === 'GLOBAL'
                        ? 'Post for everyone'
                        : `Post for ${names[audience]}`}
                  </Link>
                }
              >
                {audience === 'NEARBY'
                  ? 'Nothing open nearby yet. Be the first to post one.'
                  : `No invites ${inWords[audience]} yet. Be the first to post one.`}
              </EmptyState>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
