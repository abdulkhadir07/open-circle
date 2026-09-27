const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

const ALLOWED_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

export function validateAttachment(file: File): string | null {
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'File size exceeds the maximum allowed attachment size (10 MB).';
  }
  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return 'File type is not supported. Attach a JPEG, PNG, WebP, or PDF file.';
  }
  return null;
}
