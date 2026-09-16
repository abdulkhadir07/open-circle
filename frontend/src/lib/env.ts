/**
 * Typed access to Vite's public environment variables. All `VITE_*` values are
 * inlined into the client bundle at build time, so only non-secret, public
 * configuration belongs here.
 *
 * In development, `/api` and `/ws` are proxied to the backend by Vite (see
 * vite.config.ts), so requests can stay relative and same-origin. These
 * overrides only matter once the frontend and backend are deployed to
 * separate hosts in production.
 */
export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '',
  wsUrl: import.meta.env.VITE_WS_URL ?? '',
} as const;
