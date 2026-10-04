import { Star } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import type { ReceivedRating } from '../api/contracts';
import { formatRelativeTime } from '../lib/formatRelativeTime';

export function ReceivedRatingRow({ rating }: { rating: ReceivedRating }) {
  return (
    <Card as="li" className="animate-fade-up flex items-center gap-3 p-4">
      <ProfileAvatarLink userId={rating.raterUserId}>
        <Avatar name={rating.raterUsername} profileImage={rating.raterProfileImage} />
      </ProfileAvatarLink>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold">
          <ProfileLink userId={rating.raterUserId}>{rating.raterUsername}</ProfileLink>
        </p>
        <p className="text-muted-foreground text-xs">{formatRelativeTime(rating.revealedAt)}</p>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <span className="flex" aria-hidden="true">
          {[1, 2, 3, 4, 5].map((value) => (
            <Star
              key={value}
              className={cn(
                'size-4',
                value <= Math.round(rating.score) ? 'fill-primary text-primary' : 'text-primary/30',
              )}
            />
          ))}
        </span>
        <span
          className="text-sm font-semibold tabular-nums"
          aria-label={`${rating.score} out of 5`}
        >
          {rating.score}/5
        </span>
      </div>
    </Card>
  );
}
