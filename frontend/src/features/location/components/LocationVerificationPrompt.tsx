import { motion, useReducedMotion } from 'motion/react';
import { LoaderCircle, MapPin } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { useVerifyLocation } from '../hooks/useVerifyLocation';

const POSITION_OPTIONS: PositionOptions = {
  enableHighAccuracy: false,
  timeout: 10_000,
  maximumAge: 0,
};

const GEOLOCATION_PERMISSION_DENIED = 1;
const GEOLOCATION_POSITION_UNAVAILABLE = 2;
const GEOLOCATION_TIMEOUT = 3;

function isGeolocationPositionError(error: unknown): error is GeolocationPositionError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    typeof (error as { code: unknown }).code === 'number'
  );
}

function describeGeolocationError(error: GeolocationPositionError): string {
  switch (error.code) {
    case GEOLOCATION_PERMISSION_DENIED:
      return "Location access was blocked. Allow location access for this site in your browser's settings, then try again.";
    case GEOLOCATION_POSITION_UNAVAILABLE:
      return "Your location couldn't be determined. Check your device's location settings and try again.";
    case GEOLOCATION_TIMEOUT:
      return 'Finding your location took too long. Try again.';
    default:
      return 'Unable to get your location. Try again.';
  }
}

function getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, POSITION_OPTIONS);
  });
}

export function LocationVerificationPrompt() {
  const reduceMotion = useReducedMotion();
  const verifyLocationMutation = useVerifyLocation();
  const [locating, setLocating] = useState(false);
  const [geoError, setGeoError] = useState<string | null>(null);

  const geolocationSupported = typeof navigator !== 'undefined' && 'geolocation' in navigator;
  const pending = locating || verifyLocationMutation.isPending;
  const errorMessage = geoError ?? verifyLocationMutation.error?.message ?? null;

  async function handleShareLocation() {
    setGeoError(null);
    verifyLocationMutation.reset();
    setLocating(true);

    try {
      const position = await getCurrentPosition();
      setLocating(false);
      await verifyLocationMutation.mutateAsync({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });
    } catch (error) {
      setLocating(false);
      if (isGeolocationPositionError(error)) {
        setGeoError(describeGeolocationError(error));
      }
    }
  }

  return (
    <motion.section
      initial={reduceMotion ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: reduceMotion ? 0 : 0.4, ease: 'easeOut' }}
      className="flex max-w-md flex-col items-start gap-4"
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

      {!geolocationSupported ? (
        <p className="text-destructive text-sm" role="alert">
          Your browser doesn&apos;t support location access. Try a different browser to continue.
        </p>
      ) : (
        <>
          <Button
            type="button"
            className="h-10 px-4"
            disabled={pending}
            onClick={() => void handleShareLocation()}
          >
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
