import type { ReactNode } from 'react';

export function PageHeader({ title, sub }: { title: ReactNode; sub?: ReactNode }) {
  return (
    <div className="mb-6">
      <h1 className="text-[26px] font-extrabold tracking-tight">{title}</h1>
      {sub ? <p className="text-muted-foreground text-sm">{sub}</p> : null}
    </div>
  );
}
