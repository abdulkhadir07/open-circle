import type { ComponentProps, ElementType } from 'react';
import { cn } from '@/lib/utils';

export function Card({
  as: Component = 'div',
  className,
  ...props
}: ComponentProps<'div'> & { as?: ElementType }) {
  return (
    <Component
      data-slot="card"
      className={cn('bg-card rounded-2xl border p-5', className)}
      {...props}
    />
  );
}
