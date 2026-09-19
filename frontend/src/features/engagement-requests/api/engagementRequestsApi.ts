import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseEngagementRequest, parseEngagementRequestList } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function createEngagementRequest(invitePostId: string) {
  const response = await normalizeFailure(
    apiClient.post(`/invite-posts/${invitePostId}/engagements`),
  );
  return parseEngagementRequest(response.data);
}

export async function getMyEngagementRequests() {
  const response = await normalizeFailure(apiClient.get('/engagements/mine'));
  return parseEngagementRequestList(response.data);
}

export async function getReceivedEngagementRequests() {
  const response = await normalizeFailure(apiClient.get('/engagements/received'));
  return parseEngagementRequestList(response.data);
}

export async function acceptEngagementRequest(requestId: string) {
  const response = await normalizeFailure(apiClient.patch(`/engagements/${requestId}/accept`));
  return parseEngagementRequest(response.data);
}

export async function declineEngagementRequest(requestId: string) {
  const response = await normalizeFailure(apiClient.patch(`/engagements/${requestId}/decline`));
  return parseEngagementRequest(response.data);
}

export async function holdEngagementRequest(requestId: string) {
  const response = await normalizeFailure(apiClient.patch(`/engagements/${requestId}/hold`));
  return parseEngagementRequest(response.data);
}

export async function withdrawEngagementRequest(requestId: string) {
  const response = await normalizeFailure(apiClient.patch(`/engagements/${requestId}/withdraw`));
  return parseEngagementRequest(response.data);
}
