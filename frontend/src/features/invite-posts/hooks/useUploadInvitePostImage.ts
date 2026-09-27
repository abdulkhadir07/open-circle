import { useMutation } from '@tanstack/react-query';
import { uploadInvitePostImage } from '../api/invitePostsApi';

/**
 * Deliberately has no cache side effects of its own — a new post's images are
 * uploaded one at a time right after creation, and the caller invalidates the
 * feed once after the whole sequence settles rather than after every image.
 */
export function useUploadInvitePostImage() {
  return useMutation({
    mutationFn: uploadInvitePostImage,
  });
}
