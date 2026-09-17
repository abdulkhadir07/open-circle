import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { StrictMode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/authStore';
import { createTestQueryClient } from '@/test/render';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { authQueryKeys } from './api/queryKeys';
import { AuthBootstrapProvider } from './AuthBootstrapProvider';

function apiFailure(status: number, message: string) {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      error: status === 401 ? 'UNAUTHORIZED' : 'ERROR',
      message,
      path: '/api/auth/refresh',
      fieldErrors: {},
    },
    { status },
  );
}

function renderBootstrap(strict = false) {
  const queryClient = createTestQueryClient();
  const content = (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrapProvider>
        <p>Protected content</p>
      </AuthBootstrapProvider>
    </QueryClientProvider>
  );
  return { queryClient, ...render(strict ? <StrictMode>{content}</StrictMode> : content) };
}

describe('AuthBootstrapProvider', () => {
  afterEach(() => vi.restoreAllMocks());

  it('hydrates the token and current user before authenticating', async () => {
    const { queryClient } = renderBootstrap();

    await waitFor(() => expect(useAuthStore.getState().authStatus).toBe('authenticated'));
    expect(useAuthStore.getState().accessToken).toBe('refreshed-token');
    expect(queryClient.getQueryData(authQueryKeys.currentUser)).toEqual(authUser);
    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('treats a missing refresh session as anonymous', async () => {
    server.use(http.post('*/api/auth/refresh', () => apiFailure(401, 'No session')));
    renderBootstrap();

    await waitFor(() => expect(useAuthStore.getState().authStatus).toBe('anonymous'));
    expect(useAuthStore.getState().bootstrapError).toBeNull();
  });

  it.each([
    [403, /session check blocked/i, 'security'],
    [503, /could not reach opencircle/i, 'unavailable'],
  ] as const)(
    'keeps protected content blocked after a %s bootstrap failure',
    async (status, title, kind) => {
      server.use(http.post('*/api/auth/refresh', () => apiFailure(status, 'Bootstrap failed')));
      renderBootstrap();

      expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument();
      expect(useAuthStore.getState().authStatus).toBe('bootstrapping');
      expect(useAuthStore.getState().bootstrapError).toEqual({ kind });
      expect(screen.queryByText('Protected content')).not.toBeInTheDocument();
    },
  );

  it('becomes anonymous when users/me is still unauthorized after one refresh retry', async () => {
    let refreshCalls = 0;
    let meCalls = 0;
    server.use(
      http.post('*/api/auth/refresh', () => {
        refreshCalls += 1;
        return HttpResponse.json({ token: `token-${refreshCalls}` });
      }),
      http.get('*/api/users/me', () => {
        meCalls += 1;
        return apiFailure(401, 'Expired access token');
      }),
    );
    renderBootstrap();

    await waitFor(() => expect(useAuthStore.getState().authStatus).toBe('anonymous'));
    expect(refreshCalls).toBe(2);
    expect(meCalls).toBe(2);
  });

  it('ejects both interceptor registrations during React StrictMode effect cleanup', async () => {
    const requestUse = vi.spyOn(apiClient.interceptors.request, 'use');
    const requestEject = vi.spyOn(apiClient.interceptors.request, 'eject');
    const responseUse = vi.spyOn(apiClient.interceptors.response, 'use');
    const responseEject = vi.spyOn(apiClient.interceptors.response, 'eject');

    const view = renderBootstrap(true);
    await waitFor(() => expect(useAuthStore.getState().authStatus).toBe('authenticated'));

    expect(requestUse).toHaveBeenCalledTimes(2);
    expect(responseUse).toHaveBeenCalledTimes(2);
    expect(requestEject).toHaveBeenCalledTimes(1);
    expect(responseEject).toHaveBeenCalledTimes(1);

    view.unmount();
    expect(requestEject).toHaveBeenCalledTimes(2);
    expect(responseEject).toHaveBeenCalledTimes(2);
  });
});
