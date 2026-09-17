import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { CircleAlert } from 'lucide-react';

export function AuthFormError({ message }: { message?: string }) {
  const reduceMotion = useReducedMotion();

  return (
    <AnimatePresence initial={false}>
      {message ? (
        <motion.div
          role="alert"
          initial={reduceMotion ? false : { opacity: 0, height: 0, y: -6 }}
          animate={{ opacity: 1, height: 'auto', y: 0 }}
          exit={reduceMotion ? undefined : { opacity: 0, height: 0, y: -6 }}
          transition={{ duration: reduceMotion ? 0 : 0.22, ease: 'easeOut' }}
          className="overflow-hidden"
        >
          <div className="border-destructive/25 bg-destructive/5 text-foreground flex gap-3 rounded-md border px-3 py-3 text-sm leading-5">
            <CircleAlert aria-hidden="true" className="text-destructive mt-0.5 size-4 shrink-0" />
            <span>{message}</span>
          </div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
