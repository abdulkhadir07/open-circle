import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

type TabItem = { key: string; to: string; label: string };

/** Pill tabs where each tab is a route link (e.g. `?tab=sent`). For in-page toggles use `SegmentedControl`. */
export function Tabs({ items, active }: { items: TabItem[]; active: string }) {
  return (
    <div className="bg-card mb-5 inline-flex rounded-xl border p-1">
      {items.map((tab) => (
        <Link
          key={tab.key}
          to={tab.to}
          aria-current={tab.key === active ? 'page' : undefined}
          className={cn(
            'rounded-lg px-4 py-1.5 text-sm font-medium transition',
            tab.key === active
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          {tab.label}
        </Link>
      ))}
    </div>
  );
}
