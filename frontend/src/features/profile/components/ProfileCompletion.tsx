import { Check, Circle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

type ProfileCompletionProps = {
  hasBio: boolean;
  hasInterests: boolean;
  hasPhoto: boolean;
  onEditProfile: () => void;
};

/** Nudges the owner to finish their profile; renders nothing once everything is filled in. */
export function ProfileCompletion({
  hasBio,
  hasInterests,
  hasPhoto,
  onEditProfile,
}: ProfileCompletionProps) {
  if (hasBio && hasInterests && hasPhoto) return null;

  const steps = [
    { done: hasPhoto, label: 'Add a photo', hint: 'Click your photo above to upload one.' },
    {
      done: hasBio,
      label: 'Add a bio',
      hint: 'Tell people a bit about yourself.',
      action: 'Add a bio',
    },
    {
      done: hasInterests,
      label: 'Add interests',
      hint: 'Pick a few things you like to do.',
      action: 'Add interests',
    },
  ];
  const doneCount = steps.filter((step) => step.done).length;

  return (
    <Card className="border-primary/30 animate-fade-up">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-base font-semibold">Complete your profile</h2>
        <span className="text-muted-foreground text-xs tabular-nums">
          {doneCount} of {steps.length} done
        </span>
      </div>
      <div className="bg-muted mt-2 h-1.5 overflow-hidden rounded-full">
        <div
          className="bg-primary h-full rounded-full transition-[width] duration-500"
          style={{ width: `${(doneCount / steps.length) * 100}%` }}
        />
      </div>
      <ul className="mt-3 space-y-2">
        {steps.map((step) => (
          <li key={step.label} className="flex items-center gap-3 text-sm">
            {step.done ? (
              <Check aria-hidden="true" className="text-primary size-4 shrink-0" />
            ) : (
              <Circle aria-hidden="true" className="text-muted-foreground size-4 shrink-0" />
            )}
            <span className={step.done ? 'text-muted-foreground line-through' : 'flex-1'}>
              {step.done ? step.label : (step.hint ?? step.label)}
            </span>
            {!step.done && step.action ? (
              <Button type="button" variant="outline" size="sm" onClick={onEditProfile}>
                {step.action}
              </Button>
            ) : null}
          </li>
        ))}
      </ul>
    </Card>
  );
}
