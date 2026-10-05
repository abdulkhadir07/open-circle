import type { LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

type TabItem = { key: string; to: string; label: string; icon?: LucideIcon };

/**
 * Pill tabs where each tab is a route link (e.g. `?tab=sent`). For in-page toggles use `SegmentedControl`.
 * `compact` tightens the padding, as on the Banter board.
 */
export function Tabs({
  items,
  active,
  compact = false,
}: {
  items: TabItem[];
  active: string;
  compact?: boolean;
}) {
  return (
    <div className="bg-card mb-5 inline-flex rounded-xl border p-1">
      {items.map(({ key, to, label, icon: Icon }) => (
        <Link
          key={key}
          to={to}
          aria-current={key === active ? 'page' : undefined}
          className={cn(
            'flex items-center gap-1.5 rounded-lg text-sm font-medium transition',
            compact ? 'px-3 py-1' : 'px-4 py-1.5',
            key === active
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          {Icon ? <Icon aria-hidden="true" className="size-3.5" /> : null}
          {label}
        </Link>
      ))}
    </div>
  );
}
