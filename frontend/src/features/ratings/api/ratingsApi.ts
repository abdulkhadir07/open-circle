import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseDueRatings,
  parseRatingSubmission,
  parseReceivedRatingsPage,
  parseReputation,
} from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getDueRatings() {
  const response = await normalizeFailure(apiClient.get('/users/me/ratings/due'));
  return parseDueRatings(response.data);
}

export async function submitRating(engagementId: string, score: number) {
  const response = await normalizeFailure(
    apiClient.post(`/engagements/${engagementId}/ratings`, { score }),
  );
  return parseRatingSubmission(response.data);
}

export async function getReceivedRatings({ page, size }: { page: number; size: number }) {
  const response = await normalizeFailure(
    apiClient.get('/users/me/ratings/received', { params: { page, size } }),
  );
  return parseReceivedRatingsPage(response.data);
}

export async function getReputation(userId: string) {
  const response = await normalizeFailure(apiClient.get(`/users/${userId}/reputation`));
  return parseReputation(response.data);
}
