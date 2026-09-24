import { Eye, EyeOff } from 'lucide-react';
import { useState, type ComponentProps } from 'react';
import { Input } from '@/components/ui/input';

type PasswordInputProps = Omit<ComponentProps<typeof Input>, 'type'> & {
  label: string;
  error?: string;
};

export function PasswordInput({ id, label, error, ...props }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);
  const errorId = error && id ? `${id}-error` : undefined;

  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-foreground text-base font-medium">
        {label}
      </label>
      <div className="relative">
        <Input
          id={id}
          type={visible ? 'text' : 'password'}
          aria-invalid={Boolean(error)}
          aria-describedby={errorId}
          className="pr-11"
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((value) => !value)}
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring absolute inset-y-0 right-0 flex w-11 items-center justify-center rounded-r-md transition-colors focus-visible:ring-2 focus-visible:outline-none"
          aria-label={visible ? 'Hide password' : 'Show password'}
          title={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? (
            <EyeOff aria-hidden="true" className="size-4" />
          ) : (
            <Eye aria-hidden="true" className="size-4" />
          )}
        </button>
      </div>
      {error ? (
        <p id={errorId} role="alert" className="text-destructive text-sm">
          {error}
        </p>
      ) : null}
    </div>
  );
}
