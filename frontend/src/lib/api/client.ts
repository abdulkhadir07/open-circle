import axios from 'axios';
import { env } from '@/lib/env';

/**
 * Base HTTP client for the OpenCircle API.
 *
 * `withCredentials` is required so the browser sends the backend's httpOnly
 * refresh-token cookie. No Content-Type default is set: axios infers
 * `application/json` for plain objects and the correct multipart boundary
 * for `FormData` automatically, and forcing one here would break file
 * uploads.
 *
 * Deliberately no response interceptor here. feature/frontend-auth adds a
 * 401 refresh/retry interceptor that must see the raw AxiosError (with its
 * original request config) to replay the request after refreshing — if
 * error normalization ran as an interceptor registered here first, it would
 * always run before that one and hand it an already-converted ApiError
 * instead. `normalizeApiError` (see ./errors) stays a plain function that
 * callers — feature API functions, or a Query/Mutation cache's global
 * onError — apply themselves, after any retry has had its chance.
 */
export const apiClient = axios.create({
  baseURL: `${env.apiBaseUrl}/api`,
  withCredentials: true,
});
