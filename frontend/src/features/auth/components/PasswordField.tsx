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
    <div className="space-y-1">
      <label htmlFor={id} className="text-muted-foreground mb-1 block text-xs font-medium">
        {label}
      </label>
      <div className="relative">
        <Input
          id={id}
          type={visible ? 'text' : 'password'}
          aria-invalid={Boolean(error)}
          aria-describedby={errorId}
          className={cn('pr-11', className)}
          {...props}
        />
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
