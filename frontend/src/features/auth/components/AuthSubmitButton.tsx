import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import type { ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type AuthSubmitButtonProps = {
  pending: boolean;
  pendingLabel: string;
  icon?: ReactNode;
  className?: string;
  children: ReactNode;
} & Omit<React.ComponentProps<typeof Button>, 'children'>;

/**
 * Primary auth CTA. Adds a stronger press effect than the shared Button
 * default, and morphs its content into a slim progress bar while pending
 * instead of just swapping an icon.
 */
export function AuthSubmitButton({
  pending,
  pendingLabel,
  icon,
  className,
  children,
  disabled,
  ...props
}: AuthSubmitButtonProps) {
  const reduceMotion = useReducedMotion();

  return (
    <Button
      className={cn(
        'relative overflow-hidden transition-all active:scale-[0.98]',
        'hover:shadow-primary/25 hover:shadow-lg',
        className,
      )}
      disabled={disabled ?? pending}
      aria-busy={pending}
      {...props}
    >
      <AnimatePresence mode="wait" initial={false}>
        {pending ? (
          <motion.span
            key="pending"
            initial={reduceMotion ? false : { opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={reduceMotion ? undefined : { opacity: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.15 }}
            className="flex w-full items-center gap-3"
          >
            <span className="shrink-0 text-sm">{pendingLabel}</span>
            <span className="bg-primary-foreground/25 relative h-1 flex-1 overflow-hidden rounded-full">
              <motion.span
                className="bg-primary-foreground absolute inset-y-0 left-0 w-1/3 rounded-full"
                animate={reduceMotion ? undefined : { x: ['-10%', '220%'] }}
                transition={
                  reduceMotion ? undefined : { duration: 1.1, repeat: Infinity, ease: 'easeInOut' }
                }
              />
            </span>
          </motion.span>
        ) : (
          <motion.span
            key="idle"
            initial={reduceMotion ? false : { opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={reduceMotion ? undefined : { opacity: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.15 }}
            className="flex items-center gap-1.5"
          >
            {icon}
            {children}
          </motion.span>
        )}
      </AnimatePresence>
    </Button>
  );
}
