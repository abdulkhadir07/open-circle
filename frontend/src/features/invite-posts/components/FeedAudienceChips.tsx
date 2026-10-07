import { motion, useReducedMotion } from 'motion/react';
import { Globe, MapPin } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { FeedAudience } from '../lib/audience';

type Option = { value: FeedAudience; label: string };

/** One row that answers "whose invites am I looking at?", named after the viewer's own place. */
export function FeedAudienceChips({
  options,
  value,
  onChange,
}: {
  options: readonly Option[];
  value: FeedAudience;
  onChange: (value: FeedAudience) => void;
}) {
  const reduceMotion = useReducedMotion();

  return (
    <div
      role="radiogroup"
      aria-label="Whose invites to show"
      // Scrolls sideways on narrow screens instead of wrapping into two rows.
      className="-mx-1 mb-5 flex [scrollbar-width:none] gap-2 overflow-x-auto px-1 py-1 [&::-webkit-scrollbar]:hidden"
    >
      {options.map((option) => {
        const selected = option.value === value;
        const Icon = option.value === 'GLOBAL' ? Globe : option.value === 'NEARBY' ? MapPin : null;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              'relative flex shrink-0 cursor-pointer items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition active:scale-95',
              selected
                ? 'text-primary-foreground border-transparent'
                : 'bg-card text-muted-foreground hover:border-primary/50 hover:text-foreground',
            )}
          >
            {selected ? (
              <motion.span
                layoutId="feed-audience-pill"
                className="bg-primary absolute inset-0 rounded-full"
                transition={
                  reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 500, damping: 34 }
                }
              />
            ) : null}
            {Icon ? <Icon aria-hidden="true" className="relative size-3.5" /> : null}
            <span className="relative">{option.label}</span>
          </button>
        );
      })}
    </div>
  );
}
