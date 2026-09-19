import { LoaderCircle } from 'lucide-react';
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
    return (
      <p className="text-muted-foreground text-base">
        No one has requested to join your posts yet.
      </p>
    );
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
              personName={request.requesterUsername}
              actions={
                actionable ? (
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => acceptRequest.mutate(request.id)}
                      disabled={acceptRequest.isPending}
                      className="bg-primary text-primary-foreground hover:bg-primary/80 rounded-md px-2.5 py-1 text-sm font-medium disabled:opacity-50"
                    >
                      Accept
                    </button>
                    <button
                      type="button"
                      onClick={() => declineRequest.mutate(request.id)}
                      disabled={declineRequest.isPending}
                      className="text-muted-foreground hover:text-destructive rounded-md px-2 py-1 text-sm font-medium disabled:opacity-50"
                    >
                      Decline
                    </button>
                    {request.status === 'PENDING' ? (
                      <button
                        type="button"
                        onClick={() => holdRequest.mutate(request.id)}
                        disabled={holdRequest.isPending}
                        className="text-muted-foreground hover:text-foreground rounded-md px-2 py-1 text-sm font-medium disabled:opacity-50"
                      >
                        Hold
                      </button>
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
