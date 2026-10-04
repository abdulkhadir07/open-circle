import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseBanter,
  parseBanterLike,
  parseBanterPage,
  parseBanterReply,
  parseBanterReplyList,
  type BanterSort,
} from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getBanterBoard({
  sort,
  page,
  size,
}: {
  sort: BanterSort;
  page: number;
  size: number;
}) {
  const response = await normalizeFailure(
    apiClient.get('/banter', { params: { sort, page, size } }),
  );
  return parseBanterPage(response.data);
}

export async function createBanter(content: string) {
  const response = await normalizeFailure(apiClient.post('/banter', { content }));
  return parseBanter(response.data);
}

export async function deleteBanter(banterId: string) {
  await normalizeFailure(apiClient.delete(`/banter/${banterId}`));
}

export async function likeBanter(banterId: string) {
  const response = await normalizeFailure(apiClient.put(`/banter/${banterId}/like`));
  return parseBanterLike(response.data);
}

export async function unlikeBanter(banterId: string) {
  const response = await normalizeFailure(apiClient.delete(`/banter/${banterId}/like`));
  return parseBanterLike(response.data);
}

export async function getBanterReplies(banterId: string) {
  const response = await normalizeFailure(apiClient.get(`/banter/${banterId}/replies`));
  return parseBanterReplyList(response.data);
}

export async function createBanterReply({
  banterId,
  content,
}: {
  banterId: string;
  content: string;
}) {
  const response = await normalizeFailure(
    apiClient.post(`/banter/${banterId}/replies`, { content }),
  );
  return parseBanterReply(response.data);
}

export async function deleteBanterReply({
  banterId,
  replyId,
}: {
  banterId: string;
  replyId: string;
}) {
  await normalizeFailure(apiClient.delete(`/banter/${banterId}/replies/${replyId}`));
}
