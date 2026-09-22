import { LoaderCircle } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { useSubmitRating } from '../hooks/useSubmitRating';

const SCORES = [1, 2, 3, 4, 5] as const;

type RateEngagementDialogProps = {
  engagementId: string;
  otherUsername: string;
};

export function RateEngagementDialog({ engagementId, otherUsername }: RateEngagementDialogProps) {
  const [open, setOpen] = useState(false);
  const [score, setScore] = useState<number | null>(null);
  const [hoveredScore, setHoveredScore] = useState<number | null>(null);
  const submitRating = useSubmitRating(engagementId);

  async function handleSubmit() {
    if (!score) return;
    try {
      await submitRating.mutateAsync(score);
      setOpen(false);
    } catch {
      // Surfaced via submitRating.error below.
    }
  }

  const displayedScore = hoveredScore ?? score;

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) {
          setScore(null);
          setHoveredScore(null);
        }
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" size="sm">
          Rate
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Rate {otherUsername}</DialogTitle>
          <DialogDescription>
            Your rating stays sealed until {otherUsername} rates you back too.
          </DialogDescription>
        </DialogHeader>

        <div className="flex items-center justify-center gap-2 py-2">
          {SCORES.map((value) => (
            <button
              key={value}
              type="button"
              aria-label={`${value} out of 5`}
              aria-pressed={score === value}
              onClick={() => setScore(value)}
              onMouseEnter={() => setHoveredScore(value)}
              onMouseLeave={() => setHoveredScore(null)}
              className={cn(
                'flex size-10 items-center justify-center rounded-full border text-base font-semibold transition-colors',
                displayedScore === value
                  ? 'bg-primary border-primary text-primary-foreground'
                  : 'border-border text-muted-foreground hover:border-primary/50',
              )}
            >
              {value}
            </button>
          ))}
        </div>

        {submitRating.isError ? (
          <p role="alert" className="text-destructive text-base">
            {submitRating.error instanceof Error
              ? submitRating.error.message
              : 'Unable to submit your rating.'}
          </p>
        ) : null}

        <DialogFooter>
          <DialogClose asChild>
            <Button type="button" variant="outline" className="h-10 px-4">
              Cancel
            </Button>
          </DialogClose>
          <Button
            type="button"
            className="h-10 px-4"
            disabled={!score || submitRating.isPending}
            onClick={() => void handleSubmit()}
          >
            {submitRating.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : null}
            {submitRating.isPending ? 'Submitting' : 'Submit rating'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
