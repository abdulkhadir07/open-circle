import { MapPin } from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { SCOPE_LABELS, type InvitePost } from '../api/contracts';
import { formatTimeRemaining } from '../lib/formatTimeRemaining';

const REFRESH_INTERVAL_MS = 60_000;
const TOTAL_WINDOW_MS = 24 * 60 * 60 * 1000;
const MAX_VISIBLE_CAPACITY_DOTS = 10;

const cardShape = 'rounded-tl-3xl rounded-tr-lg rounded-br-3xl rounded-bl-lg';

type InvitePostCardProps = {
  post: InvitePost;
};

/** A small ring around the avatar that visibly drains as the post's 24h window runs out. */
function ExpiryRing({ fraction, children }: { fraction: number; children: ReactNode }) {
  const size = 40;
  const strokeWidth = 2.5;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference * (1 - fraction);

  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-muted"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className="text-primary transition-[stroke-dashoffset] duration-500 ease-linear"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">{children}</div>
    </div>
  );
}

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
            'size-2 rounded-full',
            index < acceptedCount ? 'bg-primary' : 'bg-primary/20',
          )}
        />
      ))}
    </div>
  );
}

export function InvitePostCard({ post }: InvitePostCardProps) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, []);

  const msRemaining = new Date(post.expiresAt).getTime() - now;
  const fraction = Math.min(1, Math.max(0, msRemaining / TOTAL_WINDOW_MS));
  const timeRemaining = formatTimeRemaining(msRemaining);
  const showCapacityDots = post.totalCapacity <= MAX_VISIBLE_CAPACITY_DOTS;

  return (
    <article
      className={cn(
        'border-primary/15 from-card to-primary/[0.04] border bg-gradient-to-br p-5',
        cardShape,
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <ExpiryRing fraction={fraction}>
            <span className="bg-accent/20 text-accent flex size-8 items-center justify-center rounded-full text-sm font-semibold">
              {post.posterUsername[0]?.toUpperCase()}
            </span>
          </ExpiryRing>
          <div>
            <p className="text-foreground text-sm font-semibold">{post.posterUsername}</p>
            <p className="text-muted-foreground flex items-center gap-1 text-xs">
              <MapPin aria-hidden="true" className="size-3" />
              {post.city}, {post.country}
            </p>
          </div>
        </div>
        <span className="text-muted-foreground shrink-0 text-xs tabular-nums">{timeRemaining}</span>
      </div>

      <p className="text-foreground mt-4 leading-6 whitespace-pre-wrap">{post.content}</p>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
        <span className="bg-muted text-muted-foreground rounded-full px-2.5 py-1 text-xs font-medium">
          {SCOPE_LABELS[post.locationScope]}
        </span>
        {post.inviteType === 'GROUP' ? (
          showCapacityDots ? (
            <div className="flex items-center gap-2">
              <CapacityDots totalCapacity={post.totalCapacity} acceptedCount={post.acceptedCount} />
              <span className="text-muted-foreground text-xs">{post.invitesLeft} left</span>
            </div>
          ) : (
            <span className="bg-primary/10 text-primary rounded-full px-2.5 py-1 text-xs font-medium">
              {post.invitesLeft} {post.invitesLeft === 1 ? 'invite' : 'invites'} left
            </span>
          )
        ) : null}
      </div>
    </article>
  );
}
