import 'axios';

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRefresh?: boolean;
    /**
     * A 401 from this request means something other than "the access token is
     * stale" (e.g. a wrong Hidden Chats PIN) — still attach the Authorization
     * header as normal, but don't treat the 401 as a dead session.
     */
    skipUnauthorizedRetry?: boolean;
    _retry?: boolean;
  }

  export interface InternalAxiosRequestConfig {
    skipAuthRefresh?: boolean;
    skipUnauthorizedRetry?: boolean;
    _retry?: boolean;
  }
}
