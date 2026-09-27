import { useMutation } from '@tanstack/react-query';
import { getAttachmentDownloadUrl } from '../api/attachmentApi';

export function useOpenAttachment() {
  return useMutation({
    mutationFn: async (attachmentId: string) => {
      const { url } = await getAttachmentDownloadUrl(attachmentId);
      window.open(url, '_blank', 'noopener,noreferrer');
    },
  });
}
