import { useState, type ComponentProps } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { Eye, EyeOff } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { AnimatedError } from './AnimatedError';

type PasswordFieldProps = Omit<ComponentProps<typeof Input>, 'type'> & {
  label: string;
  error?: string;
};

export function PasswordField({ id, label, error, className, ...props }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const reduceMotion = useReducedMotion();
  const errorId = error && id ? `${id}-error` : undefined;

  return (
    <div className="space-y-2">
      <div className="relative">
        <Input
          id={id}
          type={visible ? 'text' : 'password'}
          placeholder=" "
          aria-invalid={Boolean(error)}
          aria-describedby={errorId}
          className={cn('peer h-14 pt-4 pr-11 pb-1', className)}
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
        <button
          type="button"
          onClick={() => setVisible((value) => !value)}
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring absolute inset-y-0 right-0 flex w-11 items-center justify-center overflow-hidden rounded-r-md transition-colors focus-visible:ring-2 focus-visible:outline-none"
          aria-label={visible ? 'Hide password' : 'Show password'}
          title={visible ? 'Hide password' : 'Show password'}
        >
          <AnimatePresence mode="wait" initial={false}>
            <motion.span
              key={visible ? 'visible' : 'hidden'}
              initial={reduceMotion ? false : { opacity: 0, scale: 0.7 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={reduceMotion ? undefined : { opacity: 0, scale: 0.7 }}
              transition={{ duration: reduceMotion ? 0 : 0.15 }}
              className="flex"
            >
              {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
            </motion.span>
          </AnimatePresence>
        </button>
      </div>
      <AnimatedError id={errorId} message={error} />
    </div>
  );
}
