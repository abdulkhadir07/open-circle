import { Check, Clock, MapPin, Undo2, X } from 'lucide-react';
import type { ReactNode } from 'react';
import { ReputationBadge } from '@/features/ratings/components/ReputationBadge';
import { cn } from '@/lib/utils';
import type { EngagementRequest } from '../api/contracts';

type Tone = 'pending' | 'held' | 'accepted' | 'declined' | 'withdrawn';

const STATUS_PRESENTATION: Record<
  EngagementRequest['status'],
  { label: string; icon: typeof Check; tone: Tone }
> = {
  PENDING: { label: 'Pending', icon: Clock, tone: 'pending' },
  HELD: { label: 'On hold', icon: Clock, tone: 'held' },
  ACCEPTED: { label: 'Accepted', icon: Check, tone: 'accepted' },
  DECLINED: { label: 'Declined', icon: X, tone: 'declined' },
  WITHDRAWN: { label: 'Withdrawn', icon: Undo2, tone: 'withdrawn' },
};

const TONE_STYLES: Record<Tone, string> = {
  pending: 'bg-primary/10 text-primary',
  held: 'bg-blue-500/10 text-blue-700 dark:text-blue-400',
  accepted: 'bg-green-500/10 text-green-700 dark:text-green-400',
  declined: 'bg-destructive/10 text-destructive',
  withdrawn: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-400',
};

type EngagementRequestRowProps = {
  request: EngagementRequest;
  /** The other party's name — the poster for a sent row, the requester for a received row. */
  personName: string;
  /**
   * The other party's userId, for a reputation badge — omitted on the Sent
   * tab, since `request.invitePost` only carries the poster's username, not
   * their id.
   */
  reputationUserId?: string;
  /** Actionable buttons for this row (Withdraw, or Accept/Decline/Hold) — omitted once the request is final. */
  actions?: ReactNode;
};

export function EngagementRequestRow({
  request,
  personName,
  reputationUserId,
  actions,
}: EngagementRequestRowProps) {
  const { label, icon: Icon, tone } = STATUS_PRESENTATION[request.status];

  return (
    <li className="border-border bg-card flex flex-wrap items-start justify-between gap-3 rounded-xl border p-4">
      <div className="flex min-w-0 items-start gap-3">
        <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
          {personName[0]?.toUpperCase()}
        </span>
        <div className="min-w-0">
          <p className="text-foreground flex items-center gap-2 text-base font-semibold">
            {personName}
            <ReputationBadge userId={reputationUserId} />
          </p>
          <p className="text-foreground mt-0.5 line-clamp-2 text-base">
            {request.invitePost.content}
          </p>
          <p className="text-muted-foreground mt-1 flex items-center gap-1 text-sm">
            <MapPin aria-hidden="true" className="size-3" />
            {request.invitePost.city}, {request.invitePost.country}
          </p>
        </div>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-2">
        <span
          className={cn(
            'flex items-center gap-1.5 rounded-full px-2.5 py-1 text-sm font-medium',
            TONE_STYLES[tone],
          )}
        >
          <Icon aria-hidden="true" className="size-3" />
          {label}
        </span>
        {actions}
      </div>
    </li>
  );
}
