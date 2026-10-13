import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseInvitePost,
  parseInvitePostImage,
  parseInvitePostList,
  type CreateInvitePostRequest,
  type LocalFeedScope,
  type UpdateInvitePostRequest,
} from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function createInvitePost(request: CreateInvitePostRequest) {
  const response = await normalizeFailure(apiClient.post('/invite-posts', request));
  return parseInvitePost(response.data);
}

export async function updateInvitePost({
  postId,
  request,
}: {
  postId: string;
  request: UpdateInvitePostRequest;
}) {
  const response = await normalizeFailure(apiClient.patch(`/invite-posts/${postId}`, request));
  return parseInvitePost(response.data);
}

export async function deleteInvitePost(postId: string) {
  await normalizeFailure(apiClient.delete(`/invite-posts/${postId}`));
}

export async function uploadInvitePostImage({ postId, file }: { postId: string; file: File }) {
  const formData = new FormData();
  formData.set('file', file);

  const response = await normalizeFailure(
    apiClient.post(`/invite-posts/${postId}/images`, formData),
  );
  return parseInvitePostImage(response.data);
}

export async function getLocalFeed(scope?: LocalFeedScope) {
  const response = await normalizeFailure(
    apiClient.get('/invite-posts/local', { params: scope ? { scope } : undefined }),
  );
  return parseInvitePostList(response.data);
}

export async function getGlobalFeed() {
  const response = await normalizeFailure(apiClient.get('/invite-posts/global'));
  return parseInvitePostList(response.data);
}
