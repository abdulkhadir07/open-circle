import { LoaderCircle, Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { useMyEngagementRequests } from '../hooks/useMyEngagementRequests';
import { useWithdrawEngagementRequest } from '../hooks/useWithdrawEngagementRequest';
import { EngagementRequestRow } from './EngagementRequestRow';

export function SentRequestsList() {
  const requests = useMyEngagementRequests();
  const withdrawRequest = useWithdrawEngagementRequest();

  if (requests.isLoading) {
    return (
      <LoaderCircle
        aria-hidden="true"
        className="text-muted-foreground mx-auto block size-5 animate-spin"
      />
    );
  }

  if (requests.isError) {
    return (
      <p role="alert" className="text-destructive text-base">
        {requests.error instanceof Error ? requests.error.message : 'Unable to load your requests.'}
      </p>
    );
  }

  if (!requests.data || requests.data.length === 0) {
    return <EmptyState icon={Send}>You haven't requested to join anything yet.</EmptyState>;
  }

  return (
    <div className="space-y-3">
      <ul className="space-y-3">
        {requests.data.map((request) => {
          const canWithdraw = request.status === 'PENDING' || request.status === 'HELD';
          return (
            <EngagementRequestRow
              key={request.id}
              request={request}
              direction="sent"
              personName={request.invitePost.posterUsername}
              otherUserId={request.invitePost.posterId ?? undefined}
              actions={
                canWithdraw ? (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => withdrawRequest.mutate(request.id)}
                    disabled={withdrawRequest.isPending}
                  >
                    {withdrawRequest.isPending ? 'Withdrawing…' : 'Withdraw Request'}
                  </Button>
                ) : null
              }
            />
          );
        })}
      </ul>
      {withdrawRequest.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {withdrawRequest.error instanceof Error
            ? withdrawRequest.error.message
            : 'Unable to withdraw that request.'}
        </p>
      ) : null}
    </div>
  );
}
