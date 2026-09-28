import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import { parseProfileImage, parseUserProfile } from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getProfile(userId: string) {
  const response = await normalizeFailure(apiClient.get(`/users/${userId}/profile`));
  return parseUserProfile(response.data);
}

export type UpdateProfilePayload = {
  displayName: string;
  bio: string | null;
  interests: string[];
};

export async function updateMyProfile(payload: UpdateProfilePayload) {
  const response = await normalizeFailure(apiClient.put('/users/me/profile', payload));
  return parseUserProfile(response.data);
}

export async function uploadProfileImage(file: File) {
  const formData = new FormData();
  formData.set('file', file);

  const response = await normalizeFailure(apiClient.put('/users/me/profile-image', formData));
  return parseProfileImage(response.data);
}

export async function deleteProfileImage() {
  await normalizeFailure(apiClient.delete('/users/me/profile-image'));
}
