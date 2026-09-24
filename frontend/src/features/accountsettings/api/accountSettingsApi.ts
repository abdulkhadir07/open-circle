import { parseAccessToken } from '@/features/auth/api/contracts';
import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseSessions } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
};

export async function changePassword(payload: ChangePasswordPayload) {
  const response = await normalizeFailure(apiClient.put('/users/me/password', payload));
  return parseAccessToken(response.data);
}

export type RequestEmailChangePayload = {
  newEmail: string;
  currentPassword: string;
};

export async function requestEmailChange(payload: RequestEmailChangePayload) {
  await normalizeFailure(apiClient.post('/users/me/email-change', payload));
}

export async function verifyEmailChange(code: string) {
  const response = await normalizeFailure(
    apiClient.post('/users/me/email-change/verify', { code }),
  );
  return parseAccessToken(response.data);
}

export async function getSessions() {
  const response = await normalizeFailure(apiClient.get('/users/me/sessions'));
  return parseSessions(response.data);
}

export async function revokeSession(sessionId: string) {
  await normalizeFailure(apiClient.delete(`/users/me/sessions/${sessionId}`));
}

export async function revokeAllSessions() {
  await normalizeFailure(apiClient.delete('/users/me/sessions'));
}
