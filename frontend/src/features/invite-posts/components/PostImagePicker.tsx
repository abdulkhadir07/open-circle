import { ImagePlus, X } from 'lucide-react';
import { type ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import { validateInvitePostImage } from '../lib/validateInvitePostImage';

const MAX_IMAGES = 4;

export function PostImagePicker({
  value,
  onChange,
  disabled = false,
}: {
  value: File[];
  onChange: (files: File[]) => void;
  disabled?: boolean;
}) {
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const atLimit = value.length >= MAX_IMAGES;

  // Regenerated whenever the staged files change, and revoked on the way out
  // so we don't leak object URLs as photos are added and removed.
  const previews = useMemo(() => value.map((file) => URL.createObjectURL(file)), [value]);
  useEffect(() => {
    return () => {
      previews.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [previews]);

  function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || atLimit) return;

    const validationMessage = validateInvitePostImage(file);
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setError(null);
    onChange([...value, file]);
  }

  function removeImage(index: number) {
    onChange(value.filter((_, i) => i !== index));
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        {value.map((file, index) => (
          <div
            key={`${file.name}-${index}`}
            className="border-border relative size-16 shrink-0 overflow-hidden rounded-lg border"
          >
            <img src={previews[index]} alt="" className="size-full object-cover" />
            <button
              type="button"
              onClick={() => removeImage(index)}
              disabled={disabled}
              aria-label={`Remove photo ${index + 1}`}
              className="absolute top-0.5 right-0.5 rounded-full bg-black/60 p-0.5 text-white hover:bg-black/80"
            >
              <X aria-hidden="true" className="size-3" />
            </button>
          </div>
        ))}
        {atLimit ? null : (
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled}
            aria-label="Add a photo"
            className="border-border text-muted-foreground hover:border-foreground/40 hover:text-foreground flex size-16 shrink-0 flex-col items-center justify-center gap-1 rounded-lg border border-dashed"
          >
            <ImagePlus aria-hidden="true" className="size-4" />
            <span className="text-xs">Add</span>
          </button>
        )}
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleFileSelected}
        disabled={disabled}
        className="hidden"
      />
      {error ? (
        <p role="alert" className="text-destructive text-sm">
          {error}
        </p>
      ) : null}
      <p className="text-muted-foreground text-xs">
        {value.length}/{MAX_IMAGES} photos
      </p>
    </div>
  );
}
