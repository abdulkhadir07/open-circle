import { Check, Clock, Handshake, LoaderCircle, Undo2, X } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { EngagementRequest } from '../api/contracts';
import { useCreateEngagementRequest } from '../hooks/useCreateEngagementRequest';
import { useWithdrawEngagementRequest } from '../hooks/useWithdrawEngagementRequest';

type StatusPresentation = {
  label: string;
  icon: typeof Check;
  tone: 'pending' | 'held' | 'accepted' | 'declined' | 'withdrawn';
};

const STATUS_PRESENTATION: Record<EngagementRequest['status'], StatusPresentation> = {
  PENDING: { label: 'Request sent', icon: Clock, tone: 'pending' },
  HELD: { label: 'On hold', icon: Clock, tone: 'held' },
  ACCEPTED: { label: "You're in", icon: Check, tone: 'accepted' },
  DECLINED: { label: 'Declined', icon: X, tone: 'declined' },
  WITHDRAWN: { label: 'Withdrawn', icon: Undo2, tone: 'withdrawn' },
};

const TONE_STYLES: Record<StatusPresentation['tone'], string> = {
  pending: 'bg-muted text-foreground',
  held: 'bg-amber text-amber-foreground',
  accepted: 'bg-primary/15 text-primary font-semibold',
  declined: 'bg-destructive/10 text-destructive',
  withdrawn: 'bg-muted text-muted-foreground',
};

type EngageControlProps = {
  invitePostId: string;
  /** The current user's own engagement request for this post, if they've made one. */
  myRequest: EngagementRequest | undefined;
  /** Whether the post can still accept new requests (open, unexpired, not full). */
  postOpen: boolean;
};

/**
 * Once made, a request can never be re-made for the same post — even after
 * withdrawing or being declined, the backend permanently blocks a repeat
 * request (see EngagementRequestService.requireNoDuplicateRequest). So every
 * status here except the initial "no request yet" state is effectively final.
 */
export function EngageControl({ invitePostId, myRequest, postOpen }: EngageControlProps) {
  const reduceMotion = useReducedMotion();
  const createRequest = useCreateEngagementRequest();
  const withdrawRequest = useWithdrawEngagementRequest();

  if (myRequest) {
    const { label, icon: Icon, tone } = STATUS_PRESENTATION[myRequest.status];
    const canWithdraw = myRequest.status === 'PENDING' || myRequest.status === 'HELD';

    return (
      <div className="flex items-center gap-2">
        <span
          className={cn(
            'animate-pop relative flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium',
            TONE_STYLES[tone],
          )}
        >
          {tone === 'pending' ? (
            <span className="relative flex size-1.5" aria-hidden="true">
              {reduceMotion ? null : (
                <motion.span
                  className="bg-primary absolute inline-flex size-full rounded-full"
                  animate={{ opacity: [0.6, 0, 0.6], scale: [1, 2.2, 1] }}
                  transition={{ duration: 1.8, repeat: Infinity, ease: 'easeOut' }}
                />
              )}
              <span className="bg-primary relative inline-flex size-1.5 rounded-full" />
            </span>
          ) : (
            <Icon aria-hidden="true" className="size-3" />
          )}
          {label}
        </span>
        {canWithdraw ? (
          <Button
            type="button"
            variant="outline"
            onClick={() => withdrawRequest.mutate(myRequest.id)}
            disabled={withdrawRequest.isPending}
          >
            {withdrawRequest.isPending ? 'Withdrawing…' : 'Withdraw Request'}
          </Button>
        ) : null}
        {withdrawRequest.isError ? (
          <span role="alert" className="text-destructive text-sm">
            {withdrawRequest.error instanceof Error
              ? withdrawRequest.error.message
              : 'Unable to withdraw your request.'}
          </span>
        ) : null}
      </div>
    );
  }

  if (!postOpen) {
    return null;
  }

  return (
    <div className="flex items-center gap-2">
      <Button
        type="button"
        onClick={() => createRequest.mutate(invitePostId)}
        disabled={createRequest.isPending}
      >
        {createRequest.isPending ? (
          <LoaderCircle aria-hidden="true" className="animate-spin" />
        ) : (
          <Handshake aria-hidden="true" />
        )}
        Engage
      </Button>
      {createRequest.isError ? (
        <span role="alert" className="text-destructive text-sm">
          {createRequest.error instanceof Error
            ? createRequest.error.message
            : 'Unable to send your request.'}
        </span>
      ) : null}
    </div>
  );
}
