import { LoaderCircle, Monitor, Smartphone } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatRelativeTime } from '@/features/ratings/lib/formatRelativeTime';
import type { Session } from '../api/contracts';
import { formatUserAgentLabel, parseUserAgent } from '../lib/parseUserAgent';

export function SessionRow({
  session,
  onRevoke,
  revoking,
}: {
  session: Session;
  onRevoke: () => void;
  revoking: boolean;
}) {
  const { os } = parseUserAgent(session.userAgent);
  const isMobile = os === 'iOS' || os === 'Android';
  const DeviceIcon = isMobile ? Smartphone : Monitor;

  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="flex min-w-0 items-center gap-3">
        <span className="bg-muted text-muted-foreground flex size-9 shrink-0 items-center justify-center rounded-full">
          <DeviceIcon aria-hidden="true" className="size-4" />
        </span>
        <div className="min-w-0">
          <p className="text-foreground truncate text-base font-medium">
            {formatUserAgentLabel(session.userAgent)}
            {session.current ? (
              <span className="bg-primary/10 text-primary ml-2 rounded-full px-2 py-0.5 text-xs font-semibold">
                This device
              </span>
            ) : null}
          </p>
          <p className="text-muted-foreground text-sm">
            Active {formatRelativeTime(session.lastUsedAt)}
          </p>
        </div>
      </div>
      {session.current ? null : (
        <Button
          type="button"
          variant="outline"
          className="h-9 shrink-0 px-3"
          disabled={revoking}
          onClick={onRevoke}
        >
          {revoking ? <LoaderCircle aria-hidden="true" className="animate-spin" /> : null}
          Revoke
        </Button>
      )}
    </div>
  );
}
