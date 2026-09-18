import type { ComponentProps } from 'react';
import { cn } from '@/lib/utils';

export function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'border-input bg-background text-foreground placeholder:text-muted-foreground flex min-h-24 w-full rounded-md border px-3 py-2 text-base shadow-xs transition-[border-color,box-shadow] outline-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm',
        'focus-visible:border-ring focus-visible:ring-ring/20 focus-visible:ring-3',
        'aria-invalid:border-destructive aria-invalid:ring-destructive/15',
        className,
      )}
      {...props}
    />
  );
}
