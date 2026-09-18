import { z } from 'zod';
import type { ApiSchemas } from '@/types/api-types';

export type AuthUser = ApiSchemas['UserResponse'] & {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  emailVerified: boolean;
};

export type SignupRequest = ApiSchemas['SignupRequest'];
export type LoginRequest = ApiSchemas['LoginRequest'];
export type VerifyEmailRequest = ApiSchemas['VerifyEmailRequest'];
export type ResendVerificationRequest = ApiSchemas['ResendVerificationRequest'];

const userSchema = z
  .object({
    id: z.string().min(1),
    username: z.string().min(1),
    firstName: z.string().min(1),
    lastName: z.string().min(1),
    email: z.string().email(),
    emailVerified: z.boolean(),
  })
  .passthrough();

const authResponseSchema = z.object({
  token: z.string().min(1),
  user: userSchema,
});

const signupResponseSchema = z.object({ user: userSchema });
const accessTokenResponseSchema = z.object({ token: z.string().min(1) });

export class ApiContractError extends Error {
  constructor() {
    super('The server returned an unexpected response. Please try again.');
    this.name = 'ApiContractError';
  }
}

function parseWithContract<T>(schema: z.ZodType<T>, value: unknown): T {
  const result = schema.safeParse(value);
  if (!result.success) {
    throw new ApiContractError();
  }
  return result.data;
}

export function parseAuthResponse(value: unknown): { token: string; user: AuthUser } {
  return parseWithContract(authResponseSchema, value) as { token: string; user: AuthUser };
}

export function parseSignupResponse(value: unknown): { user: AuthUser } {
  return parseWithContract(signupResponseSchema, value) as { user: AuthUser };
}

export function parseAccessToken(value: unknown): string {
  return parseWithContract(accessTokenResponseSchema, value).token;
}

export function parseCurrentUser(value: unknown): AuthUser {
  return parseWithContract(userSchema, value) as AuthUser;
}
