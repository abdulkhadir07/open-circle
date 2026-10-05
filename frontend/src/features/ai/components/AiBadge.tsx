import { Sparkles } from 'lucide-react';
import { cn } from '@/lib/utils';

/** A small "AI" marker for anything the model wrote. */
export function AiBadge({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'bg-primary/10 text-primary inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 text-[10px] font-semibold',
        className,
      )}
    >
      <Sparkles aria-hidden="true" className="size-3" />
      AI
    </span>
  );
}
