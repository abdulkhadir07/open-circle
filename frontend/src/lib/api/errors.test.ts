import { describe, expect, it } from 'vitest';
import { ApiError, isContentBlockedError } from './errors';

function apiError(status: number) {
  return new ApiError({
    timestamp: new Date().toISOString(),
    status,
    error: 'X',
    message: 'm',
    path: '/x',
    fieldErrors: {},
  });
}

describe('isContentBlockedError', () => {
  it('is true only for a 422 API error', () => {
    expect(isContentBlockedError(apiError(422))).toBe(true);
    expect(isContentBlockedError(apiError(400))).toBe(false);
    expect(isContentBlockedError(apiError(429))).toBe(false);
  });

  it('is false for anything that is not an API error', () => {
    expect(isContentBlockedError(new Error('x'))).toBe(false);
    expect(isContentBlockedError(null)).toBe(false);
    expect(isContentBlockedError(undefined)).toBe(false);
  });
});
