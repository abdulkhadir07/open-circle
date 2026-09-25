import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseAuthResponse,
  parseCurrentUser,
  parseSignupResponse,
  type AuthUser,
  type ForgotPasswordRequest,
  type LoginRequest,
  type ResendVerificationRequest,
  type ResetPasswordRequest,
  type SignupRequest,
  type VerifyEmailRequest,
} from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function signup(request: SignupRequest) {
  const response = await normalizeFailure(
    apiClient.post('/auth/signup', request, { skipAuthRefresh: true }),
  );
  return parseSignupResponse(response.data);
}

export async function login(request: LoginRequest) {
  const response = await normalizeFailure(
    apiClient.post('/auth/login', request, { skipAuthRefresh: true }),
  );
  return parseAuthResponse(response.data);
}

export async function verifyEmail(request: VerifyEmailRequest) {
  const response = await normalizeFailure(
    apiClient.post('/auth/verify-email', request, { skipAuthRefresh: true }),
  );
  return parseAuthResponse(response.data);
}

export async function resendVerification(request: ResendVerificationRequest) {
  await normalizeFailure(
    apiClient.post('/auth/resend-verification', request, { skipAuthRefresh: true }),
  );
}

export async function forgotPassword(request: ForgotPasswordRequest) {
  await normalizeFailure(
    apiClient.post('/auth/forgot-password', request, { skipAuthRefresh: true }),
  );
}

export async function resetPassword(request: ResetPasswordRequest) {
  await normalizeFailure(
    apiClient.post('/auth/reset-password', request, { skipAuthRefresh: true }),
  );
}

export async function logout() {
  await normalizeFailure(apiClient.post('/auth/logout', undefined, { skipAuthRefresh: true }));
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await normalizeFailure(apiClient.get('/users/me'));
  return parseCurrentUser(response.data);
}
