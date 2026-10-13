import { z } from 'zod';

export const MIN_USERNAME_LENGTH = 3;
export const MAX_USERNAME_LENGTH = 30;
export const USERNAME_PATTERN = /^[A-Za-z0-9]+(?:[._][A-Za-z0-9]+)*$/;

export const usernameSchema = z
  .string()
  .trim()
  .min(MIN_USERNAME_LENGTH, 'Username must be at least 3 characters')
  .max(MAX_USERNAME_LENGTH, 'Username must be 30 characters or fewer')
  .regex(USERNAME_PATTERN, 'Use only letters, numbers, periods, and underscores');
