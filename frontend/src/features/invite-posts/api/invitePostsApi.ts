import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseInvitePost,
  parseInvitePostList,
  type CreateInvitePostRequest,
  type LocalFeedScope,
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
