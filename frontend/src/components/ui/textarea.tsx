import type { ComponentProps } from 'react';
import { cn } from '@/lib/utils';

export function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'border-input bg-card text-foreground placeholder:text-muted-foreground flex min-h-24 w-full rounded-lg border px-3 py-2 text-sm transition outline-none disabled:cursor-not-allowed disabled:opacity-50',
        'focus-visible:ring-primary/40 focus-visible:ring-2 focus-visible:outline-none',
        'aria-invalid:border-destructive aria-invalid:ring-destructive/15',
        className,
      )}
      {...props}
    />
  );
}
