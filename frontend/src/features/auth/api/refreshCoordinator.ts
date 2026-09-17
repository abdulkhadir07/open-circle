import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { useAuthStore } from '@/stores/authStore';
import { parseAccessToken } from './contracts';

let refreshPromise: Promise<string> | null = null;

export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post('/auth/refresh', undefined, { skipAuthRefresh: true })
      .then((response) => {
        const token = parseAccessToken(response.data);
        useAuthStore.getState().setAccessToken(token);
        return token;
      })
      .catch((error: unknown) => {
        useAuthStore.getState().setAccessToken(null);
        throw normalizeApiError(error);
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}
