import type { ComponentProps, ReactNode } from 'react';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
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

  // Native date inputs render their own "mm/dd/yyyy" chrome regardless of
  // :placeholder-shown, so a floating label fights the browser instead of
  // working with it. Keep those static-labeled.
  if (props.type === 'date') {
    return (
      <div className="space-y-2">
        <label htmlFor={id} className="text-foreground block text-sm font-medium">
          {label}
        </label>
        <Input
          id={id}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          className={cn(className)}
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

  return (
    <div className="space-y-2">
      <div className="relative">
        <Input
          id={id}
          placeholder=" "
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          className={cn('peer h-14 pt-4 pb-1', className)}
          {...props}
        />
        <label
          htmlFor={id}
          className={cn(
            'text-muted-foreground pointer-events-none absolute top-1/2 left-3 -translate-y-1/2',
            'text-base transition-all duration-150 ease-out',
            'peer-focus:text-primary peer-focus:top-3.5 peer-focus:translate-y-0 peer-focus:text-xs',
            'peer-[:not(:placeholder-shown)]:top-3.5 peer-[:not(:placeholder-shown)]:translate-y-0 peer-[:not(:placeholder-shown)]:text-xs',
          )}
        >
          {label}
        </label>
      </div>
      {hint ? (
        <p id={hintId} className="text-muted-foreground text-xs leading-5">
          {hint}
        </p>
      ) : null}
      <AnimatedError id={errorId} message={error} />
    </div>
  );
}
