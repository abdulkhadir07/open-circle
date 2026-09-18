import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { queryClient } from '@/lib/queryClient';
import { useAuthStore } from '@/stores/authStore';
import { server } from './mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  cleanup();
  server.resetHandlers();
  queryClient.clear();
  useAuthStore.setState({
    accessToken: null,
    authStatus: 'bootstrapping',
    bootstrapError: null,
  });
});

afterAll(() => server.close());

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: (query: string): MediaQueryList => ({
    matches: query.includes('prefers-reduced-motion'),
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
  }),
});

document.elementFromPoint = () => null;

// jsdom doesn't implement these, but Radix's Select relies on them for its
// pointer-driven open/scroll behaviour.
window.HTMLElement.prototype.hasPointerCapture ??= () => false;
window.HTMLElement.prototype.releasePointerCapture ??= () => undefined;
window.HTMLElement.prototype.scrollIntoView ??= () => undefined;
