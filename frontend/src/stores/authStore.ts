import { create } from 'zustand';

export type AuthStatus = 'bootstrapping' | 'anonymous' | 'authenticated';
export type BootstrapError = { kind: 'security' | 'unavailable' };

type AuthState = {
  accessToken: string | null;
  authStatus: AuthStatus;
  bootstrapError: BootstrapError | null;
  beginBootstrap: () => void;
  setAccessToken: (accessToken: string | null) => void;
  setAuthenticated: (accessToken?: string) => void;
  setAnonymous: () => void;
  failBootstrap: (error: BootstrapError) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  authStatus: 'bootstrapping',
  bootstrapError: null,
  beginBootstrap: () =>
    set({
      accessToken: null,
      authStatus: 'bootstrapping',
      bootstrapError: null,
    }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setAuthenticated: (accessToken) =>
    set((state) => ({
      accessToken: accessToken ?? state.accessToken,
      authStatus: 'authenticated',
      bootstrapError: null,
    })),
  setAnonymous: () =>
    set({
      accessToken: null,
      authStatus: 'anonymous',
      bootstrapError: null,
    }),
  failBootstrap: (bootstrapError) =>
    set({
      accessToken: null,
      authStatus: 'bootstrapping',
      bootstrapError,
    }),
}));
