import type { ComponentProps, ReactNode } from 'react';
import { Input } from '@/components/ui/input';
import { AnimatedError } from './AnimatedError';

type AuthFormFieldProps = ComponentProps<typeof Input> & {
  label: string;
  error?: string;
  hint?: ReactNode;
};

export function AuthFormField({ id, label, error, hint, className, ...props }: AuthFormFieldProps) {
  const errorId = error && id ? `${id}-error` : undefined;
  const hintId = hint && id ? `${id}-hint` : undefined;
  const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined;

  return (
    <div className="space-y-1">
      <label htmlFor={id} className="text-muted-foreground mb-1 block text-xs font-medium">
        {label}
      </label>
      <Input
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy}
        className={className}
        {...props}
      />
      {hint ? (
        <p id={hintId} className="text-muted-foreground text-xs leading-5">
          {hint}
        </p>
      ) : null}
      <AnimatedError id={errorId} message={error} />
    </div>
  );
}
