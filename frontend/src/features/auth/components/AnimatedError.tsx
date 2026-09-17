import { AnimatePresence, motion, useReducedMotion } from 'motion/react';

type AnimatedErrorProps = {
  id?: string;
  message?: string;
};

/** Slides/fades a field or form error in and out. Height-animated so nothing jumps. */
export function AnimatedError({ id, message }: AnimatedErrorProps) {
  const reduceMotion = useReducedMotion();

  return (
    <AnimatePresence initial={false}>
      {message ? (
        <motion.p
          id={id}
          role="alert"
          initial={reduceMotion ? false : { opacity: 0, height: 0, y: -4 }}
          animate={{ opacity: 1, height: 'auto', y: 0 }}
          exit={reduceMotion ? undefined : { opacity: 0, height: 0, y: -4 }}
          transition={{ duration: reduceMotion ? 0 : 0.2, ease: 'easeOut' }}
          className="text-destructive text-sm"
        >
          {message}
        </motion.p>
      ) : null}
    </AnimatePresence>
  );
}
