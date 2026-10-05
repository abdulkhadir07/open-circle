import { Clock, Sparkles, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { Card } from '@/components/ui/card';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { cn } from '@/lib/utils';
import type { EngagementRequest } from '@/features/engagement-requests/api/contracts';
import { EngageControl } from '@/features/engagement-requests/components/EngageControl';
import type { InvitePost } from '../api/contracts';
import { formatTimeRemaining } from '../lib/formatTimeRemaining';
import { PostImageCarousel } from './PostImageCarousel';

const REFRESH_INTERVAL_MS = 60_000;
const URGENT_WINDOW_MS = 3 * 60 * 60 * 1000;
const MAX_VISIBLE_CAPACITY_DOTS = 10;

type InvitePostCardProps = {
  post: InvitePost;
  /** Whether the current viewer is this post's own poster. Defaults to false. */
  isOwnPost?: boolean;
  /** The current viewer's own engagement request for this post, if they've made one. */
  myRequest?: EngagementRequest;
  /** When given, topic chips become buttons that call it (the Home feed filters by topic). */
  onTagClick?: (tag: string) => void;
  /** Why this invite suits the viewer (from the AI feed insights), when there is a match. */
  reason?: string;
};

/** Visualizes group capacity as filled (taken) vs. open seats — falls back to a plain count past a certain size. */
function CapacityDots({
  totalCapacity,
  acceptedCount,
}: {
  totalCapacity: number;
  acceptedCount: number;
}) {
  return (
    <div className="flex items-center gap-1" aria-hidden="true">
      {Array.from({ length: totalCapacity }, (_, index) => (
        <span
          key={index}
          className={cn(
            'size-2.5 rounded-full',
            index < acceptedCount ? 'bg-primary' : 'border-primary/50 border',
          )}
        />
      ))}
    </div>
  );
}

export function InvitePostCard({
  post,
  isOwnPost = false,
  myRequest,
  onTagClick,
  reason,
}: InvitePostCardProps) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, []);

  const msRemaining = new Date(post.expiresAt).getTime() - now;
  const timeRemaining = formatTimeRemaining(msRemaining);
  const urgent = msRemaining < URGENT_WINDOW_MS;
  const showCapacityDots = post.totalCapacity <= MAX_VISIBLE_CAPACITY_DOTS;
  const postOpen = post.status === 'ACTIVE' && msRemaining > 0 && post.invitesLeft > 0;
  const spotsLeft =
    post.invitesLeft === 0
      ? 'Full'
      : post.invitesLeft === 1
        ? '1 spot left'
        : `${post.invitesLeft} spots left`;

  return (
    <Card
      as="article"
      className="hover:border-primary/40 transition duration-200 hover:-translate-y-0.5 hover:shadow-md"
    >
      <div className="mb-3 flex items-center gap-3">
        <ProfileAvatarLink userId={post.posterId}>
          <Avatar name={post.posterUsername} profileImage={post.posterProfileImage} />
        </ProfileAvatarLink>
        <p className="min-w-0 flex-1 truncate text-sm font-semibold">
          <ProfileLink userId={post.posterId}>{post.posterUsername}</ProfileLink>
        </p>
        <span
          className={cn(
            'flex shrink-0 items-center gap-1 rounded-full px-2 py-1 text-xs tabular-nums',
            urgent ? 'bg-primary/15 text-primary font-medium' : 'bg-muted text-muted-foreground',
          )}
        >
          <Clock aria-hidden="true" className="size-3" />
          {timeRemaining}
        </span>
      </div>

      <p className="text-[15px] leading-relaxed whitespace-pre-wrap">{post.content}</p>

      {reason && !isOwnPost ? (
        <p className="text-primary mt-2 flex items-center gap-1 text-xs font-medium">
          <Sparkles aria-hidden="true" className="size-3 shrink-0" />
          {reason}
        </p>
      ) : null}

      <PostImageCarousel images={post.images} />

      {post.tags.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {post.tags.map((tag) => {
            const chipClass =
              'bg-primary/10 text-primary rounded-full px-2.5 py-0.5 text-xs font-medium';
            return onTagClick ? (
              <button
                key={tag}
                type="button"
                onClick={() => onTagClick(tag)}
                className={cn(chipClass, 'hover:bg-primary/20 cursor-pointer transition')}
              >
                #{tag}
              </button>
            ) : (
              <span key={tag} className={chipClass}>
                #{tag}
              </span>
            );
          })}
        </div>
      ) : null}

      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm">
        <span className="flex items-center gap-1">
          <Users aria-hidden="true" className="text-primary size-3.5" />
          {post.inviteType === 'SINGLE' ? 'Just one person' : 'Group'}
        </span>
      </div>

      <div className="mt-4 flex items-center justify-between gap-3 border-t pt-4">
        <div className="text-muted-foreground flex items-center gap-2 text-sm">
          {showCapacityDots ? (
            <CapacityDots totalCapacity={post.totalCapacity} acceptedCount={post.acceptedCount} />
          ) : null}
          <span>{spotsLeft}</span>
        </div>
        {isOwnPost ? (
          <span className="text-muted-foreground text-xs">Your invite</span>
        ) : (
          <EngageControl invitePostId={post.id} myRequest={myRequest} postOpen={postOpen} />
        )}
      </div>
    </Card>
  );
}
