import { isAxiosError, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { apiClient } from '@/lib/api/client';
import { queryClient } from '@/lib/queryClient';
import { useAuthStore } from '@/stores/authStore';
import { authQueryKeys } from './queryKeys';
import { refreshAccessToken } from './refreshCoordinator';

function canRefresh(
  error: AxiosError,
): error is AxiosError & { config: InternalAxiosRequestConfig } {
  const request = error.config;
  return Boolean(
    error.response?.status === 401 && request && !request.skipAuthRefresh && !request._retry,
  );
}

export function setupAuthInterceptors() {
  const requestInterceptor = apiClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().accessToken;
    if (token && !config.skipAuthRefresh) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  const responseInterceptor = apiClient.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
      if (!isAxiosError(error) || !canRefresh(error)) {
        return Promise.reject(error);
      }

      const originalRequest = error.config;
      originalRequest._retry = true;

      try {
        const token = await refreshAccessToken();
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return await apiClient.request(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().setAnonymous();
        queryClient.removeQueries({ queryKey: authQueryKeys.all });
        return Promise.reject(refreshError);
      }
    },
  );

  return () => {
    apiClient.interceptors.request.eject(requestInterceptor);
    apiClient.interceptors.response.eject(responseInterceptor);
  };
}
