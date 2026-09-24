import { LoaderCircle, LogOut } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { useRevokeAllSessions } from '../hooks/useRevokeAllSessions';
import { useRevokeSession } from '../hooks/useRevokeSession';
import { useSessions } from '../hooks/useSessions';
import { SessionRow } from './SessionRow';

export function SessionsList() {
  const sessions = useSessions();
  const revokeSession = useRevokeSession();
  const revokeAllSessions = useRevokeAllSessions();
  const [revokingId, setRevokingId] = useState<string | null>(null);

  function handleRevoke(sessionId: string) {
    setRevokingId(sessionId);
    revokeSession.mutate(sessionId, { onSettled: () => setRevokingId(null) });
  }

  if (sessions.isLoading) {
    return (
      <div className="flex justify-center py-8">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-5 animate-spin" />
      </div>
    );
  }

  const data = sessions.data ?? [];
  const hasOtherSessions = data.some((session) => !session.current);

  return (
    <div className="border-border bg-card rounded-xl border p-4">
      <div className="flex items-center justify-between gap-4">
        <p className="text-muted-foreground text-sm">
          Devices currently signed in to your account.
        </p>
        <Button
          type="button"
          variant="outline"
          className="h-9 shrink-0 px-3"
          disabled={revokeAllSessions.isPending}
          onClick={() => revokeAllSessions.mutate()}
        >
          {revokeAllSessions.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <LogOut aria-hidden="true" className="size-4" />
          )}
          Sign out everywhere
        </Button>
      </div>

      <div className="divide-border mt-2 divide-y">
        {data.map((session) => (
          <SessionRow
            key={session.id}
            session={session}
            onRevoke={() => handleRevoke(session.id)}
            revoking={revokingId === session.id && revokeSession.isPending}
          />
        ))}
      </div>

      {!hasOtherSessions && data.length <= 1 ? (
        <p className="text-muted-foreground mt-2 text-sm">
          No other devices are currently signed in.
        </p>
      ) : null}
    </div>
  );
}
