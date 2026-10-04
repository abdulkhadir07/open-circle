import { useEffect, useState } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { Card } from '@/components/ui/card';
import type { DueRating } from '../api/contracts';
import { formatDueCountdown } from '../lib/formatDueCountdown';
import { copyForTrigger } from '../lib/ratingTriggerCopy';
import { StarRatingPicker } from './StarRatingPicker';

const REFRESH_INTERVAL_MS = 60_000;

export function DueRatingRow({ dueRating }: { dueRating: DueRating }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, []);

  const msRemaining = new Date(dueRating.dueAt).getTime() - now;

  return (
    <Card as="li" className="animate-fade-up flex flex-wrap items-center gap-3 p-4">
      <ProfileAvatarLink userId={dueRating.otherUserId}>
        <Avatar name={dueRating.otherUsername} profileImage={dueRating.otherUserProfileImage} />
      </ProfileAvatarLink>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold">
          <ProfileLink userId={dueRating.otherUserId}>{dueRating.otherUsername}</ProfileLink>
        </p>
        <p className="text-muted-foreground truncate text-xs">
          {copyForTrigger(dueRating.trigger)}
        </p>
        <p className="text-muted-foreground mt-0.5 text-xs tabular-nums">
          {formatDueCountdown(msRemaining)}
        </p>
      </div>
      <StarRatingPicker
        engagementId={dueRating.engagementId}
        otherUsername={dueRating.otherUsername}
      />
    </Card>
  );
}
