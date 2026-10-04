import { Star } from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';
import { useSubmitRating } from '../hooks/useSubmitRating';

const SCORES = [1, 2, 3, 4, 5] as const;
const LABELS = ['', 'Not great', 'Okay', 'Good', 'Really good', 'Amazing!'];

type StarRatingPickerProps = {
  engagementId: string;
  otherUsername: string;
};

/** Rates immediately on click, like a quick tap — the rating stays sealed until the other person rates back. */
export function StarRatingPicker({ engagementId, otherUsername }: StarRatingPickerProps) {
  const [hovered, setHovered] = useState(0);
  const [picked, setPicked] = useState(0);
  const submitRating = useSubmitRating(engagementId);
  const shown = picked || hovered;

  if (submitRating.isSuccess) {
    return (
      <span className="animate-pop bg-primary/15 text-primary rounded-full px-3 py-1 text-sm font-semibold">
        Thanks! Sealed until {otherUsername} rates you back.
      </span>
    );
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-2">
        <span className="text-muted-foreground hidden w-20 text-right text-xs font-medium sm:block">
          {LABELS[shown]}
        </span>
        <div className="flex" onMouseLeave={() => setHovered(0)}>
          {SCORES.map((value) => (
            <button
              key={value}
              type="button"
              aria-label={`${value} out of 5`}
              title={LABELS[value]}
              disabled={submitRating.isPending}
              onMouseEnter={() => setHovered(value)}
              onClick={() => {
                setPicked(value);
                submitRating.mutate(value, { onError: () => setPicked(0) });
              }}
              className="cursor-pointer p-0.5 transition active:scale-90 disabled:cursor-default"
            >
              <Star
                aria-hidden="true"
                className={cn(
                  'size-7 transition',
                  value <= shown ? 'fill-primary text-primary scale-110' : 'text-primary/30',
                )}
              />
            </button>
          ))}
        </div>
      </div>
      {submitRating.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {submitRating.error instanceof Error
            ? submitRating.error.message
            : 'Unable to submit your rating.'}
        </p>
      ) : null}
    </div>
  );
}
