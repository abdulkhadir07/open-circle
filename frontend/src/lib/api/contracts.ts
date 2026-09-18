import type { z } from 'zod';

/** Thrown when a response doesn't match the shape a feature's Zod schema expects. */
export class ApiContractError extends Error {
  constructor() {
    super('The server returned an unexpected response. Please try again.');
    this.name = 'ApiContractError';
  }
}

export function parseWithContract<T>(schema: z.ZodType<T>, value: unknown): T {
  const result = schema.safeParse(value);
  if (!result.success) {
    throw new ApiContractError();
  }
  return result.data;
}
