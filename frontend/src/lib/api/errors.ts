import axios from 'axios';

/**
 * Mirrors the backend's ApiError response shape exactly
 * (backend/src/main/java/com/opencircle/common/ApiError.java).
 */
export type ApiErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
};

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(response: ApiErrorResponse) {
    super(response.message);
    this.name = 'ApiError';
    this.status = response.status;
    this.fieldErrors = response.fieldErrors;
  }
}

function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  return (
    typeof data === 'object' &&
    data !== null &&
    'status' in data &&
    'message' in data &&
    'fieldErrors' in data
  );
}

/** Normalizes any Axios failure into a consistent ApiError, even if the backend never responded. */
export function normalizeApiError(error: unknown): ApiError {
  if (!axios.isAxiosError(error)) {
    return new ApiError({
      timestamp: new Date().toISOString(),
      status: 0,
      error: 'CLIENT_ERROR',
      message: error instanceof Error ? error.message : 'An unexpected error occurred',
      path: '',
      fieldErrors: {},
    });
  }

  if (isApiErrorResponse(error.response?.data)) {
    return new ApiError(error.response.data);
  }

  return new ApiError({
    timestamp: new Date().toISOString(),
    status: error.response?.status ?? 0,
    error: 'NETWORK_ERROR',
    message: error.message,
    path: error.config?.url ?? '',
    fieldErrors: {},
  });
}
