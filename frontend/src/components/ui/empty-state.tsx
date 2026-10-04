import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

export function EmptyState({ icon: Icon, children }: { icon: LucideIcon; children: ReactNode }) {
  return (
    <div className="text-muted-foreground flex flex-col items-center gap-2 rounded-2xl border-2 border-dashed p-10 text-center text-sm">
      <Icon aria-hidden="true" className="size-6" />
      <p>{children}</p>
    </div>
  );
}
