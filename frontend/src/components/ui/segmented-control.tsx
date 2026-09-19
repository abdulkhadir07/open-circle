import { motion, useReducedMotion } from 'motion/react';
import type { ComponentType } from 'react';
import { cn } from '@/lib/utils';

type SegmentedControlOption<Value extends string> = {
  value: Value;
  label: string;
  icon?: ComponentType<{ className?: string }>;
};

type SegmentedControlProps<Value extends string> = {
  'aria-label': string;
  options: readonly SegmentedControlOption<Value>[];
  value: Value;
  onChange: (value: Value) => void;
  className?: string;
  /** `sm` reads as a secondary/nested control — smaller and quieter than the default. */
  size?: 'md' | 'sm';
};

export function SegmentedControl<Value extends string>({
  'aria-label': ariaLabel,
  options,
  value,
  onChange,
  className,
  size = 'md',
}: SegmentedControlProps<Value>) {
  const reduceMotion = useReducedMotion();
  // Unique per control instance — every call site passes a distinct label,
  // which keeps motion's shared-layout pill from morphing between unrelated
  // controls that happen to render at the same time.
  const layoutId = `segmented-pill-${ariaLabel}`;

  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel}
      className={cn(
        'inline-flex gap-1 rounded-lg p-1',
        size === 'sm' ? 'bg-muted/60' : 'bg-muted',
        className,
      )}
    >
      {options.map((option) => {
        const selected = option.value === value;
        const Icon = option.icon;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              'relative flex items-center gap-1.5 rounded-md font-medium whitespace-nowrap',
              size === 'sm' ? 'px-2.5 py-1 text-xs' : 'px-3 py-1.5 text-sm',
              selected ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {selected ? (
              <motion.span
                layoutId={layoutId}
                className="bg-card absolute inset-0 rounded-md shadow-sm"
                transition={
                  reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 500, damping: 34 }
                }
              />
            ) : null}
            {Icon ? (
              <Icon
                aria-hidden="true"
                className={cn('relative', size === 'sm' ? 'size-3.5' : 'size-4')}
              />
            ) : null}
            <span className="relative">{option.label}</span>
          </button>
        );
      })}
    </div>
  );
}
