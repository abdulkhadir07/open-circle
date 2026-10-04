import { Inbox, LoaderCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { useAcceptEngagementRequest } from '../hooks/useAcceptEngagementRequest';
import { useDeclineEngagementRequest } from '../hooks/useDeclineEngagementRequest';
import { useHoldEngagementRequest } from '../hooks/useHoldEngagementRequest';
import { useReceivedEngagementRequests } from '../hooks/useReceivedEngagementRequests';
import { EngagementRequestRow } from './EngagementRequestRow';

export function ReceivedRequestsList() {
  const requests = useReceivedEngagementRequests();
  const acceptRequest = useAcceptEngagementRequest();
  const declineRequest = useDeclineEngagementRequest();
  const holdRequest = useHoldEngagementRequest();

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
    return <EmptyState icon={Inbox}>No one has requested to join your posts yet.</EmptyState>;
  }

  const actionError = acceptRequest.error ?? declineRequest.error ?? holdRequest.error;

  return (
    <div className="space-y-3">
      <ul className="space-y-3">
        {requests.data.map((request) => {
          const actionable = request.status === 'PENDING' || request.status === 'HELD';
          return (
            <EngagementRequestRow
              key={request.id}
              request={request}
              direction="received"
              personName={request.requesterUsername}
              otherUserId={request.requesterId}
              personProfileImage={request.requesterProfileImage}
              actions={
                actionable ? (
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      onClick={() => acceptRequest.mutate(request.id)}
                      disabled={acceptRequest.isPending}
                    >
                      Accept
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => declineRequest.mutate(request.id)}
                      disabled={declineRequest.isPending}
                    >
                      Decline
                    </Button>
                    {request.status === 'PENDING' ? (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => holdRequest.mutate(request.id)}
                        disabled={holdRequest.isPending}
                      >
                        Hold
                      </Button>
                    ) : null}
                  </div>
                ) : null
              }
            />
          );
        })}
      </ul>
      {actionError ? (
        <p role="alert" className="text-destructive text-sm">
          {actionError instanceof Error ? actionError.message : 'Unable to update that request.'}
        </p>
      ) : null}
    </div>
  );
}
