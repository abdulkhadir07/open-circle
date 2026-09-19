import { LoaderCircle } from 'lucide-react';
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
    return (
      <p className="text-muted-foreground text-base">You haven't requested to join anything yet.</p>
    );
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
              personName={request.invitePost.posterUsername}
              actions={
                canWithdraw ? (
                  <button
                    type="button"
                    onClick={() => withdrawRequest.mutate(request.id)}
                    disabled={withdrawRequest.isPending}
                    className="rounded-full bg-red-800 px-3 py-1 text-sm font-bold text-white hover:bg-red-900 disabled:opacity-50"
                  >
                    {withdrawRequest.isPending ? 'Withdrawing…' : 'Withdraw Request'}
                  </button>
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
