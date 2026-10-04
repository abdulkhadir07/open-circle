import { LoaderCircle, MessageCircle } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import type { InvitePost } from '@/features/invite-posts/api/contracts';
import { useCampusFeed } from '@/features/invite-posts/hooks/useCampusFeed';
import { formatTimeRemaining } from '@/features/invite-posts/lib/formatTimeRemaining';

const MAX_OPEN_INVITES = 3;

type ProfileOpenInvitesProps = {
  userId: string;
  isOwnProfile: boolean;
  displayName: string;
};

function spotsLeft(post: InvitePost): string {
  if (post.invitesLeft === 0) return 'Full';
  return post.invitesLeft === 1 ? '1 spot left' : `${post.invitesLeft} spots left`;
}

/**
 * A person's latest open invites, as compact rows (like openSFSU's profile). There's no
 * per-user endpoint, so this reads the same campus feed as Home and keeps the posts they
 * wrote — which is also exactly the set the viewer is allowed to see.
 */
export function ProfileOpenInvites({ userId, isOwnProfile, displayName }: ProfileOpenInvitesProps) {
  const [now] = useState(() => Date.now());
  const feed = useCampusFeed();

  const posts = useMemo(
    () =>
      (feed.data ?? [])
        .filter((post) => post.posterId === userId)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .slice(0, MAX_OPEN_INVITES),
    [feed.data, userId],
  );

  const loading = feed.isLoading;
  const failed = feed.isError;

  return (
    <section>
      <h2 className="text-muted-foreground mb-2 px-1 text-xs font-semibold tracking-wide uppercase">
        {isOwnProfile ? 'Your open invites' : `${displayName}'s open invites`}
      </h2>
      {loading ? (
        <div className="flex justify-center py-6">
          <LoaderCircle aria-hidden="true" className="text-muted-foreground size-5 animate-spin" />
        </div>
      ) : posts.length > 0 ? (
        <ul className="space-y-2">
          {posts.map((post) => (
            <Card as="li" key={post.id} className="flex items-center justify-between gap-3 p-4">
              <div className="min-w-0">
                <p className="truncate font-semibold">{post.content}</p>
                <p className="text-muted-foreground truncate text-xs">
                  {[
                    post.inviteType === 'SINGLE' ? 'Just one person' : 'Group',
                    spotsLeft(post),
                  ].join(' · ')}
                </p>
              </div>
              <span className="bg-muted text-muted-foreground shrink-0 rounded-full px-2 py-1 text-xs tabular-nums">
                {formatTimeRemaining(new Date(post.expiresAt).getTime() - now)}
              </span>
            </Card>
          ))}
        </ul>
      ) : failed ? (
        <p role="alert" className="text-destructive text-base">
          Unable to load open invites.
        </p>
      ) : (
        <EmptyState icon={MessageCircle}>
          {isOwnProfile ? (
            <>
              Nothing open right now.{' '}
              <Link to="/new" className="text-primary font-medium hover:underline">
                Start an invite
              </Link>
            </>
          ) : (
            'Nothing open right now.'
          )}
        </EmptyState>
      )}
    </section>
  );
}
