import { Fragment } from 'react';
import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

const stepNames = ['About you', 'Contact', 'Location', 'Security'] as const;

export function SignupProgress({ currentStep }: { currentStep: number }) {
  return (
    <div className="mb-8" aria-label={`Step ${currentStep + 1} of ${stepNames.length}`}>
      <div className="mb-4 flex items-center justify-between">
        <span className="text-foreground text-sm font-semibold">{stepNames[currentStep]}</span>
        <span className="text-muted-foreground text-sm">
          Step {currentStep + 1} of {stepNames.length}
        </span>
      </div>
      <div className="flex items-center" aria-hidden="true">
        {stepNames.map((step, index) => (
          <Fragment key={step}>
            <div
              className={cn(
                'flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-colors duration-300',
                index < currentStep && 'bg-primary text-primary-foreground',
                index === currentStep && 'bg-primary/15 text-primary ring-primary ring-2',
                index > currentStep && 'bg-muted text-muted-foreground',
              )}
            >
              {index < currentStep ? <Check aria-hidden="true" className="size-3.5" /> : index + 1}
            </div>
            {index < stepNames.length - 1 ? (
              <div
                className={cn(
                  'mx-1.5 h-0.5 flex-1 rounded-full transition-colors duration-300',
                  index < currentStep ? 'bg-primary' : 'bg-muted',
                )}
              />
            ) : null}
          </Fragment>
        ))}
      </div>
    </div>
  );
}
