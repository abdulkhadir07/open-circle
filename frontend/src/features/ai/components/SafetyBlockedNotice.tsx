import { ShieldAlert } from 'lucide-react';

/** Shown when Safety Guardian stops something from posting; says what to change. */
export function SafetyBlockedNotice({ message }: { message: string }) {
  return (
    <div
      role="alert"
      className="border-destructive/30 bg-destructive/5 flex items-start gap-2.5 rounded-xl border p-3 text-sm"
    >
      <ShieldAlert aria-hidden="true" className="text-destructive mt-0.5 size-4 shrink-0" />
      <div>
        <p className="font-semibold">Safety Guardian paused this</p>
        <p className="text-muted-foreground">{message}</p>
      </div>
    </div>
  );
}
