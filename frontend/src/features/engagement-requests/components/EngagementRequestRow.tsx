import type { ReactNode } from 'react';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { Avatar } from '@/components/ui/avatar';
import { ReputationBadge } from '@/features/ratings/components/ReputationBadge';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import type { EngagementRequest } from '../api/contracts';

const STATUS_PRESENTATION: Record<
  EngagementRequest['status'],
  { label: string; className: string }
> = {
  PENDING: { label: 'Pending', className: 'bg-muted text-muted-foreground' },
  HELD: { label: 'On hold', className: 'bg-amber text-amber-foreground' },
  ACCEPTED: { label: 'Accepted', className: 'bg-primary/15 text-primary' },
  DECLINED: { label: 'Declined', className: 'bg-destructive/10 text-destructive' },
  WITHDRAWN: { label: 'Withdrawn', className: 'bg-muted text-muted-foreground' },
};

type EngagementRequestRowProps = {
  request: EngagementRequest;
  /** The other party's name — the poster for a sent row, the requester for a received row. */
  personName: string;
  /**
   * The other party's userId, for a profile link and reputation badge.
   * Absent on the Sent tab only when talking to a backend that predates
   * `invitePost.posterId`.
   */
  otherUserId?: string;
  /**
   * The other party's photo — only available on the Received tab, since the
   * Sent tab's `invitePost` summary carries no profile-image field (a
   * backend gap, same reason `otherUserId` is omitted there too).
   */
  personProfileImage?: { url: string } | null;
  /** Whether this row is a request someone sent you (received) or one you sent (sent). */
  direction: 'received' | 'sent';
  /** Actionable buttons for this row (Withdraw, or Accept/Decline/Hold) — omitted once the request is final. */
  actions?: ReactNode;
};

export function EngagementRequestRow({
  request,
  personName,
  otherUserId,
  personProfileImage,
  direction,
  actions,
}: EngagementRequestRowProps) {
  const { label, className } = STATUS_PRESENTATION[request.status];

  return (
    <Card as="li" className="animate-fade-up flex flex-wrap items-center gap-3 p-4">
      {otherUserId ? (
        <ProfileAvatarLink userId={otherUserId}>
          <Avatar name={personName} profileImage={personProfileImage} />
        </ProfileAvatarLink>
      ) : (
        <Avatar name={personName} profileImage={personProfileImage} />
      )}
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-1.5 text-sm font-semibold">
          {otherUserId ? <ProfileLink userId={otherUserId}>{personName}</ProfileLink> : personName}
          <ReputationBadge userId={otherUserId} />
        </p>
        <p className="text-muted-foreground truncate text-xs">
          {direction === 'sent' ? 'You asked to join ' : 'wants to join '}
          <span>{request.invitePost.content}</span>
        </p>
      </div>
      <span className={cn('rounded-full px-2.5 py-1 text-xs font-medium', className)}>{label}</span>
      {actions}
    </Card>
  );
}
