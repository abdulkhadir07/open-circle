import { z } from 'zod';
import { apiClient } from '@/lib/api/client';
import { parseWithContract } from '@/lib/api/contracts';
import { normalizeApiError } from '@/lib/api/errors';

const downloadUrlSchema = z.object({
  url: z.string().min(1),
  expiresAt: z.string().min(1),
});

export type AttachmentDownloadUrl = z.infer<typeof downloadUrlSchema>;

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getAttachmentDownloadUrl(attachmentId: string) {
  const response = await normalizeFailure(
    apiClient.get(`/attachments/${attachmentId}/download-url`),
  );
  return parseWithContract(downloadUrlSchema, response.data);
}
