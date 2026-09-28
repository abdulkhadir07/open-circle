import { LoaderCircle } from 'lucide-react';
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { Avatar } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { ApiError } from '@/lib/api/errors';
import { useDeleteProfileImage } from '../hooks/useDeleteProfileImage';
import { useUploadProfileImage } from '../hooks/useUploadProfileImage';
import { validateProfileImage } from '../lib/validateProfileImage';

type EditableProfileAvatarProps = {
  name: string;
  profileImage?: { url: string } | null;
  className?: string;
};

/** The clickable avatar on the viewer's own profile page — opens a menu to change or remove the photo. */
export function EditableProfileAvatar({
  name,
  profileImage,
  className,
}: EditableProfileAvatarProps) {
  const uploadImage = useUploadProfileImage();
  const deleteImage = useDeleteProfileImage();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [validationError, setValidationError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // Revoked once the upload settles (or on unmount), matching the cleanup
  // used for the invite-post image picker's staged previews.
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const message = validateProfileImage(file);
    if (message) {
      setValidationError(message);
      return;
    }

    setValidationError(null);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    uploadImage.mutate(file, {
      onSettled: () => {
        URL.revokeObjectURL(objectUrl);
        setPreviewUrl(null);
      },
    });
  }

  const busy = uploadImage.isPending || deleteImage.isPending;
  const shownProfileImage = previewUrl ? { url: previewUrl } : profileImage;
  const errorMessage =
    validationError ??
    (uploadImage.isError
      ? uploadImage.error instanceof ApiError
        ? uploadImage.error.message
        : 'Unable to upload that photo.'
      : deleteImage.isError
        ? deleteImage.error instanceof ApiError
          ? deleteImage.error.message
          : 'Unable to remove that photo.'
        : null);

  return (
    <div className="relative shrink-0">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label="Edit profile photo"
            disabled={busy}
            className="focus-visible:ring-ring rounded-full outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-70"
          >
            <Avatar name={name} profileImage={shownProfileImage} className={className} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          <DropdownMenuItem onSelect={() => fileInputRef.current?.click()}>
            {profileImage ? 'Change photo' : 'Upload photo'}
          </DropdownMenuItem>
          {profileImage ? (
            <DropdownMenuItem
              onSelect={() => deleteImage.mutate()}
              className="text-destructive data-[highlighted]:bg-destructive/10"
            >
              Remove photo
            </DropdownMenuItem>
          ) : null}
        </DropdownMenuContent>
      </DropdownMenu>

      {uploadImage.isPending ? (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-black/40"
        >
          <LoaderCircle className="size-5 animate-spin text-white" />
        </span>
      ) : null}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleFileSelected}
        className="hidden"
      />

      {errorMessage ? (
        <p
          role="alert"
          className="text-destructive absolute top-full left-0 mt-1 w-48 text-xs leading-snug"
        >
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
