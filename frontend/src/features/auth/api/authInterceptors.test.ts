import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/authStore';
import { server } from '@/test/mocks/server';
import { setupAuthInterceptors } from './authInterceptors';

describe('auth interceptors', () => {
  let cleanup: (() => void) | undefined;

  afterEach(() => cleanup?.());

  it('attaches the latest in-memory access token', async () => {
    let authorization: string | null = null;
    server.use(
      http.get('*/api/protected', ({ request }) => {
        authorization = request.headers.get('authorization');
        return HttpResponse.json({ ok: true });
      }),
    );
    useAuthStore.getState().setAccessToken('latest-token');
    cleanup = setupAuthInterceptors();

    await apiClient.get('/protected');
    expect(authorization).toBe('Bearer latest-token');
  });

  it('uses one refresh for concurrent 401 responses and retries each request once', async () => {
    let refreshCalls = 0;
    let protectedCalls = 0;
    server.use(
      http.post('*/api/auth/refresh', async () => {
        refreshCalls += 1;
        await new Promise((resolve) => setTimeout(resolve, 10));
        return HttpResponse.json({ token: 'new-token' });
      }),
      http.get('*/api/protected', ({ request }) => {
        protectedCalls += 1;
        if (request.headers.get('authorization') !== 'Bearer new-token') {
          return new HttpResponse(null, { status: 401 });
        }
        return HttpResponse.json({ ok: true });
      }),
    );
    useAuthStore.getState().setAuthenticated('expired-token');
    cleanup = setupAuthInterceptors();

    const responses = await Promise.all([apiClient.get('/protected'), apiClient.get('/protected')]);

    expect(responses.every((response) => response.status === 200)).toBe(true);
    expect(refreshCalls).toBe(1);
    expect(protectedCalls).toBe(4);
  });

  it('does not refresh a public auth request marked skipAuthRefresh', async () => {
    let refreshCalls = 0;
    server.use(
      http.post('*/api/auth/login', () => new HttpResponse(null, { status: 401 })),
      http.post('*/api/auth/refresh', () => {
        refreshCalls += 1;
        return HttpResponse.json({ token: 'unexpected' });
      }),
    );
    cleanup = setupAuthInterceptors();

    await expect(
      apiClient.post('/auth/login', {}, { skipAuthRefresh: true }),
    ).rejects.toBeDefined();
    expect(refreshCalls).toBe(0);
  });

  it('does not loop or replay the request when refresh fails', async () => {
    let protectedCalls = 0;
    let refreshCalls = 0;
    server.use(
      http.get('*/api/protected', () => {
        protectedCalls += 1;
        return new HttpResponse(null, { status: 401 });
      }),
      http.post('*/api/auth/refresh', () => {
        refreshCalls += 1;
        return new HttpResponse(null, { status: 401 });
      }),
    );
    useAuthStore.getState().setAuthenticated('expired-token');
    cleanup = setupAuthInterceptors();

    await expect(apiClient.get('/protected')).rejects.toBeDefined();
    expect(protectedCalls).toBe(1);
    expect(refreshCalls).toBe(1);
    expect(useAuthStore.getState().authStatus).toBe('anonymous');
  });
});
