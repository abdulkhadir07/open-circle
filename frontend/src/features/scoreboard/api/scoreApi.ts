import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseScoreSummary, parseScoreboard } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getMyScore() {
  const response = await normalizeFailure(apiClient.get('/users/me/score'));
  return parseScoreSummary(response.data);
}

export async function getScoreboard() {
  const response = await normalizeFailure(apiClient.get('/scoreboard'));
  return parseScoreboard(response.data);
}
