import { motion, useReducedMotion } from 'motion/react';
import { LoaderCircle, MapPin } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useShareLocation } from '../hooks/useShareLocation';

export function LocationVerificationPrompt() {
  const reduceMotion = useReducedMotion();
  const { shareLocation, supported, pending, errorMessage } = useShareLocation();

  return (
    <motion.section
      initial={reduceMotion ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: reduceMotion ? 0 : 0.4, ease: 'easeOut' }}
      className="bg-card mx-auto flex max-w-md flex-col items-center gap-4 rounded-2xl border p-8 text-center"
    >
      <div className="relative flex size-11 items-center justify-center">
        {!reduceMotion ? (
          <>
            <motion.span
              aria-hidden="true"
              className="bg-primary/25 absolute inset-0 rounded-full"
              animate={{ scale: [1, 1.9], opacity: [0.7, 0] }}
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeOut' }}
            />
            <motion.span
              aria-hidden="true"
              className="bg-primary/25 absolute inset-0 rounded-full"
              animate={{ scale: [1, 1.9], opacity: [0.7, 0] }}
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeOut', delay: 1.1 }}
            />
          </>
        ) : null}
        <span className="bg-primary/10 text-primary relative flex size-11 items-center justify-center rounded-full">
          <MapPin aria-hidden="true" className="size-5" />
        </span>
      </div>
      <div className="space-y-1.5">
        <h1 className="text-foreground text-2xl font-semibold">Verify your location</h1>
        <p className="text-muted-foreground">
          OpenCircle shows you invite posts from people nearby. Share your location once to see your
          local feed and post your own invites.
        </p>
      </div>

      {!supported ? (
        <p className="text-destructive text-sm" role="alert">
          Your browser doesn&apos;t support location access. Try a different browser to continue.
        </p>
      ) : (
        <>
          <Button type="button" disabled={pending} onClick={() => void shareLocation()}>
            {pending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : (
              <MapPin aria-hidden="true" />
            )}
            {pending ? 'Locating' : 'Share my location'}
          </Button>
          {errorMessage ? (
            <p className="text-destructive text-sm" role="alert">
              {errorMessage}
            </p>
          ) : null}
        </>
      )}
    </motion.section>
  );
}
