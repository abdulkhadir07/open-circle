import { ArrowLeft } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

export function SettingsSectionLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/settings"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back to Settings
      </Link>

      <p className="text-primary mt-4 text-sm font-semibold">Your account</p>
      <h1 className="text-foreground text-3xl font-semibold">{title}</h1>

      <div className="mt-6">{children}</div>
    </div>
  );
}
