import { useState } from 'react';
import { useVerifyLocation } from './useVerifyLocation';

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

/** Asks the browser for the device location and verifies it with the backend. */
export function useShareLocation() {
  const verifyLocationMutation = useVerifyLocation();
  const [locating, setLocating] = useState(false);
  const [geoError, setGeoError] = useState<string | null>(null);

  async function shareLocation() {
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

  return {
    shareLocation,
    supported: typeof navigator !== 'undefined' && 'geolocation' in navigator,
    pending: locating || verifyLocationMutation.isPending,
    errorMessage: geoError ?? verifyLocationMutation.error?.message ?? null,
  };
}
