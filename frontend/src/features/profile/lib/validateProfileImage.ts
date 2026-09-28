const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

const ALLOWED_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export function validateProfileImage(file: File): string | null {
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'Image exceeds the maximum allowed size (5 MB).';
  }
  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return 'Image type is not supported. Use a JPEG, PNG, or WebP file.';
  }
  return null;
}
