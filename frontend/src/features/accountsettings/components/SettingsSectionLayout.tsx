import { ArrowLeft } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/ui/page-header';

export function SettingsSectionLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <Link
        to="/settings"
        className="text-muted-foreground hover:text-foreground mb-4 flex items-center gap-1.5 text-sm font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back to Settings
      </Link>

      <PageHeader title={title} />

      {children}
    </div>
  );
}
