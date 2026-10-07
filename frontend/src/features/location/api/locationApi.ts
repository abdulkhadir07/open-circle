import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseCurrentUser, type AuthUser } from '@/features/auth/api/contracts';
import type { VerifyLocationRequest } from './contracts';

export async function verifyLocation(request: VerifyLocationRequest): Promise<AuthUser> {
  try {
    const response = await apiClient.put('/users/me/location', request);
    return parseCurrentUser(response.data);
  } catch (error) {
    throw normalizeApiError(error);
  }
}
