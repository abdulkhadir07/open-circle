import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { LoaderCircle, LogOut, MapPin, MessageCircle, Sparkles, Star, Users } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { LocationVerificationPrompt } from '@/features/location/components/LocationVerificationPrompt';
import { CreatePostDialog } from '@/features/invite-posts/components/CreatePostDialog';
import { InvitePostCard } from '@/features/invite-posts/components/InvitePostCard';
import { useGlobalFeed } from '@/features/invite-posts/hooks/useGlobalFeed';
import { useLocalFeed } from '@/features/invite-posts/hooks/useLocalFeed';
import { SCOPE_LABELS, type LocalFeedScope } from '@/features/invite-posts/api/contracts';
import { useMyEngagementRequests } from '@/features/engagement-requests/hooks/useMyEngagementRequests';
import { NotificationBell } from '@/features/notifications/components/NotificationBell';

const FEED_MODE_OPTIONS = [
  { value: 'local', label: 'Local' },
  { value: 'global', label: 'Global' },
] as const;

type LocalScopeFilter = 'ALL' | LocalFeedScope;

type SignOutButtonProps = {
  pending: boolean;
  onSignOut: () => void;
  className?: string;
};

function SignOutButton({ pending, onSignOut, className }: SignOutButtonProps) {
  return (
    <Button
      type="button"
      variant="outline"
      className={className}
      onClick={onSignOut}
      disabled={pending}
    >
      {pending ? (
        <LoaderCircle aria-hidden="true" className="animate-spin" />
      ) : (
        <LogOut aria-hidden="true" />
      )}
      {pending ? 'Signing out' : 'Sign out'}
    </Button>
  );
}

export function HomePage() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const logout = useLogout();
  const [feedMode, setFeedMode] = useState<'local' | 'global'>('local');
  const [localScope, setLocalScope] = useState<LocalScopeFilter>('ALL');

  const locationVerified = Boolean(currentUser.data?.locationVerifiedAt);
  const stateRegionAvailable = Boolean(currentUser.data?.verifiedStateRegion);
  const localScopeOptions = useMemo(
    () =>
      (['ALL', 'CITY', 'STATE_REGION', 'COUNTRY'] as const)
        .filter((scope) => scope !== 'STATE_REGION' || stateRegionAvailable)
        .map((scope) => ({
          value: scope,
          label: scope === 'ALL' ? 'All' : SCOPE_LABELS[scope],
        })),
    [stateRegionAvailable],
  );

  // These run on every render regardless of the early returns below (Rules
  // of Hooks), including while the location-verification prompt is still
  // showing — `enabled` is what actually stops them from hitting the API
  // (and caching a 403) before there's a verified location to query with.
  const localFeed = useLocalFeed(localScope === 'ALL' ? undefined : localScope, {
    enabled: locationVerified,
  });
  const globalFeed = useGlobalFeed({ enabled: locationVerified });
  const activeFeed = feedMode === 'local' ? localFeed : globalFeed;
  const myRequests = useMyEngagementRequests({ enabled: locationVerified });
  const myRequestsByPostId = useMemo(
    () => new Map(myRequests.data?.map((request) => [request.invitePostId, request])),
    [myRequests.data],
  );

  async function handleLogout() {
    await logout.mutateAsync().catch(() => undefined);
    navigate('/login', { replace: true });
  }

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

  return (
    <div className="lg:grid lg:grid-cols-[240px_minmax(0,1fr)_260px] lg:items-start lg:gap-10">
      {/* Identity + primary action rail — desktop only; on mobile these
          same actions live inline with the feed below instead. */}
      <aside className="sticky top-8 hidden lg:flex lg:h-[calc(100svh-6rem)] lg:flex-col">
        <div className="flex items-center gap-3">
          <span className="bg-primary/10 text-primary flex size-11 shrink-0 items-center justify-center rounded-full text-base font-semibold">
            {user.firstName?.[0]?.toUpperCase()}
          </span>
          <div className="min-w-0">
            <p className="text-foreground truncate text-base font-semibold">
              {user.firstName} {user.lastName}
            </p>
            <p className="text-muted-foreground truncate text-sm">{user.email}</p>
          </div>
        </div>

        <CreatePostDialog triggerClassName="mt-6 w-full justify-center" />

        <Button asChild variant="outline" className="mt-3 w-full justify-center">
          <Link to="/requests">
            <Users aria-hidden="true" />
            Requests
          </Link>
        </Button>

        <Button asChild variant="outline" className="mt-3 w-full justify-center">
          <Link to="/chats">
            <MessageCircle aria-hidden="true" />
            Chats
          </Link>
        </Button>

        <NotificationBell triggerClassName="mt-3 w-full justify-center" />

        <Button asChild variant="outline" className="mt-3 w-full justify-center">
          <Link to="/ratings">
            <Star aria-hidden="true" />
            Ratings
          </Link>
        </Button>

        <div className="border-border mt-auto border-t pt-4">
          <SignOutButton
            pending={logout.isPending}
            onSignOut={() => void handleLogout()}
            className="w-full justify-center"
          />
        </div>
      </aside>

      <div className="flex flex-col gap-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-2">
            <p className="text-primary text-sm font-semibold">Your circle</p>
            <h1 className="text-foreground text-3xl font-semibold">
              Welcome back{user.firstName ? `, ${user.firstName}` : ''}
            </h1>
          </div>
          <SignOutButton
            pending={logout.isPending}
            onSignOut={() => void handleLogout()}
            className="h-10 px-4 lg:hidden"
          />
        </div>

        <CreatePostDialog triggerClassName="w-full justify-center lg:hidden" />

        <Button asChild variant="outline" className="w-full justify-center lg:hidden">
          <Link to="/requests">
            <Users aria-hidden="true" />
            Requests
          </Link>
        </Button>

        <Button asChild variant="outline" className="w-full justify-center lg:hidden">
          <Link to="/chats">
            <MessageCircle aria-hidden="true" />
            Chats
          </Link>
        </Button>

        <NotificationBell triggerClassName="w-full justify-center lg:hidden" />

        <Button asChild variant="outline" className="w-full justify-center lg:hidden">
          <Link to="/ratings">
            <Star aria-hidden="true" />
            Ratings
          </Link>
        </Button>

        <div>
          <div
            role="radiogroup"
            aria-label="Feed"
            className="border-border flex items-baseline gap-7 border-b"
          >
            {FEED_MODE_OPTIONS.map((option) => {
              const selected = option.value === feedMode;
              return (
                <button
                  key={option.value}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => setFeedMode(option.value)}
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
                      layoutId="feed-mode-underline"
                      className="bg-primary absolute inset-x-0 bottom-0 h-[2.5px] rounded-full"
                      transition={
                        reduceMotion
                          ? { duration: 0 }
                          : { duration: 0.35, ease: [0.22, 1, 0.36, 1] }
                      }
                    />
                  ) : null}
                </button>
              );
            })}
          </div>

          <AnimatePresence initial={false}>
            {feedMode === 'local' ? (
              <motion.div
                key="scope"
                initial={reduceMotion ? false : { height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={reduceMotion ? undefined : { height: 0, opacity: 0 }}
                transition={{ duration: reduceMotion ? 0 : 0.25, ease: 'easeOut' }}
                className="overflow-hidden"
              >
                <div
                  role="radiogroup"
                  aria-label="Local feed scope"
                  className="flex flex-wrap items-center gap-x-3 gap-y-1 pt-4"
                >
                  {localScopeOptions.map((option, index) => {
                    const selected = option.value === localScope;
                    return (
                      <span key={option.value} className="flex items-center gap-3">
                        {index > 0 ? (
                          <span aria-hidden="true" className="text-border select-none">
                            ·
                          </span>
                        ) : null}
                        <button
                          type="button"
                          role="radio"
                          aria-checked={selected}
                          onClick={() => setLocalScope(option.value)}
                          className={cn(
                            'text-base font-medium transition-colors',
                            selected
                              ? 'text-primary'
                              : 'text-muted-foreground hover:text-foreground',
                          )}
                        >
                          {option.label}
                        </button>
                      </span>
                    );
                  })}
                </div>
              </motion.div>
            ) : null}
          </AnimatePresence>
        </div>

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
          ) : activeFeed.data && activeFeed.data.length > 0 ? (
            <motion.div
              key={`${feedMode}-${localScope}`}
              exit={reduceMotion ? undefined : { opacity: 0 }}
              className="flex flex-col gap-4"
            >
              {activeFeed.data.map((post, index) => (
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
                  />
                </motion.div>
              ))}
            </motion.div>
          ) : (
            <motion.div
              key="empty"
              exit={reduceMotion ? undefined : { opacity: 0 }}
              className="border-border flex flex-col items-center gap-2 rounded-2xl border border-dashed py-12 text-center"
            >
              <Sparkles aria-hidden="true" className="text-muted-foreground size-5" />
              <p className="text-muted-foreground text-base">
                No invite posts here yet. Be the first to post one.
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Location context rail — desktop only; genuinely explains why the
          feed shows what it shows, using data already on the user record. */}
      <aside className="sticky top-8 hidden lg:block">
        <div className="border-border bg-card rounded-2xl border p-5">
          <div className="flex items-center gap-2">
            <MapPin aria-hidden="true" className="text-primary size-4" />
            <span className="text-foreground text-base font-semibold">Your location</span>
          </div>
          <p className="text-foreground mt-2 text-base">
            {user.verifiedCity}, {user.verifiedCountry}
          </p>
          <p className="text-muted-foreground mt-3 text-sm leading-5">
            Your feed and posts are matched to this location.
          </p>
        </div>
      </aside>
    </div>
  );
}
