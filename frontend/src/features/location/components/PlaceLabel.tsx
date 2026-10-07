import { LoaderCircle, MapPin } from 'lucide-react';
import { useShareLocation } from '../hooks/useShareLocation';

/** Where the viewer's feed is centred, with a way to re-check it after moving. */
export function PlaceLabel({ place }: { place: string }) {
  const { shareLocation, supported, pending, errorMessage } = useShareLocation();

  return (
    <div className="-mt-3 mb-5 flex flex-wrap items-center gap-x-2 text-sm">
      <MapPin aria-hidden="true" className="text-primary size-4" />
      <span className="text-foreground font-medium">{place}</span>
      {supported ? (
        <button
          type="button"
          disabled={pending}
          onClick={() => void shareLocation()}
          className="text-primary flex cursor-pointer items-center gap-1 text-xs font-medium hover:underline disabled:cursor-default disabled:opacity-60"
        >
          {pending ? <LoaderCircle aria-hidden="true" className="size-3 animate-spin" /> : null}
          {pending ? 'Updating' : 'Update'}
        </button>
      ) : null}
      {errorMessage ? (
        <p role="alert" className="text-destructive w-full text-xs">
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
