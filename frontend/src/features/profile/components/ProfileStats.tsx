import { Calendar, Medal, Star, Trophy, TrendingUp, Users, type LucideIcon } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { formatAverageRating } from '@/lib/formatRating';
import type { ProfileReputation } from '../api/contracts';

type ProfileStatsProps = {
  reputation: ProfileReputation;
  awardsCount: number;
  memberSince: string;
  /** Own profile only: the viewer's current-season Circle Points. */
  points?: number;
  /** Own profile only: the viewer's season rank, when the backend provides one. */
  rank?: number | null;
};

function formatMemberSince(iso: string): string {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short' });
}

type TileProps = {
  icon: LucideIcon;
  value: string;
  label: string;
  sub?: string;
  className?: string;
};

function Tile({ icon: Icon, value, label, sub, className }: TileProps) {
  return (
    <Card
      className={cn('animate-fade-up flex flex-col items-center gap-1 p-4 text-center', className)}
    >
      <span className="bg-primary/10 text-primary flex size-9 items-center justify-center rounded-full">
        <Icon aria-hidden="true" className="size-4.5" />
      </span>
      <p className="text-xl font-bold tabular-nums">{value}</p>
      <p className="text-muted-foreground text-xs font-medium">{label}</p>
      {sub ? <p className="text-muted-foreground text-xs">{sub}</p> : null}
    </Card>
  );
}

export function ProfileStats({
  reputation,
  awardsCount,
  memberSince,
  points,
  rank,
}: ProfileStatsProps) {
  const hasRatings = reputation.totalRatingsReceived > 0;

  const tiles: Omit<TileProps, 'className'>[] = [
    {
      icon: Star,
      value: hasRatings ? `${formatAverageRating(reputation.averageRating)}/5` : '—',
      label: 'Average rating',
    },
    {
      icon: Users,
      value: String(reputation.totalRatingsReceived),
      label: reputation.totalRatingsReceived === 1 ? 'Rating' : 'Ratings',
      sub: hasRatings
        ? `from ${reputation.distinctRaterCount} ${
            reputation.distinctRaterCount === 1 ? 'person' : 'people'
          }`
        : undefined,
    },
    { icon: Trophy, value: String(awardsCount), label: awardsCount === 1 ? 'Award' : 'Awards' },
    { icon: Calendar, value: formatMemberSince(memberSince), label: 'Member since' },
  ];
  if (points !== undefined) {
    tiles.push({
      icon: TrendingUp,
      value: String(points),
      label: 'Season points',
      sub: 'Circle Points',
    });
  }
  if (typeof rank === 'number') {
    tiles.push({ icon: Medal, value: `#${rank}`, label: 'Season rank' });
  }

  // Keep every row full: 4 tiles sit four across; 5 become a 3 + 2 split; 6 are 3 + 3.
  const count = tiles.length;
  const lastIndex = count - 1;

  return (
    <div
      className={cn(
        'grid grid-cols-2 gap-3',
        count === 4 && 'sm:grid-cols-4',
        count === 5 && 'sm:grid-cols-6',
        count === 6 && 'sm:grid-cols-3',
      )}
    >
      {tiles.map((tile, index) => (
        <Tile
          key={tile.label}
          {...tile}
          className={cn(
            // An odd count leaves a lone tile on the two-column mobile grid, so let it span the row.
            count % 2 === 1 && index === lastIndex && 'col-span-2',
            count === 5 && (index < 3 ? 'sm:col-span-2' : 'sm:col-span-3'),
          )}
        />
      ))}
    </div>
  );
}
