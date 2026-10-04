import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseInvitePost,
  parseInvitePostImage,
  parseInvitePostList,
  type CreateInvitePostRequest,
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

export async function uploadInvitePostImage({ postId, file }: { postId: string; file: File }) {
  const formData = new FormData();
  formData.set('file', file);

  const response = await normalizeFailure(
    apiClient.post(`/invite-posts/${postId}/images`, formData),
  );
  return parseInvitePostImage(response.data);
}

/** Open invites from the signed-in user's campus, newest first. */
export async function getCampusFeed() {
  const response = await normalizeFailure(apiClient.get('/invite-posts/campus'));
  return parseInvitePostList(response.data);
}
