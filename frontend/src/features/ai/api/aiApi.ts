import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseFeedInsights, parseInviteDraft } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function draftInvite(text: string) {
  const response = await normalizeFailure(apiClient.post('/ai/invite-draft', { text }));
  return parseInviteDraft(response.data);
}

export async function getFeedInsights() {
  const response = await normalizeFailure(apiClient.get('/ai/feed-insights'));
  return parseFeedInsights(response.data);
}
