import { useEffect, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { CircleAlert, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/api/errors';
import { useAuthStore, type BootstrapError } from '@/stores/authStore';
import { getCurrentUser } from './api/authApi';
import { setupAuthInterceptors } from './api/authInterceptors';
import { authQueryKeys } from './api/queryKeys';
import { refreshAccessToken } from './api/refreshCoordinator';

function classifyBootstrapError(error: unknown): BootstrapError | null {
  if (error instanceof ApiError && error.status === 401) {
    return null;
  }
  if (error instanceof ApiError && error.status === 403) {
    return { kind: 'security' };
  }
  return { kind: 'unavailable' };
}

function BootstrapFailure({ error, onRetry }: { error: BootstrapError; onRetry: () => void }) {
  const securityFailure = error.kind === 'security';

  return (
    <main className="bg-background flex min-h-svh items-center justify-center px-5 py-12">
      <div role="alert" className="flex w-full max-w-md flex-col items-start gap-5">
        <div className="bg-accent text-accent-foreground flex size-11 items-center justify-center rounded-lg">
          <CircleAlert aria-hidden="true" className="size-5" />
        </div>
        <div className="space-y-2">
          <p className="text-primary text-sm font-semibold">OpenCircle</p>
          <h1 className="text-foreground text-2xl font-semibold">
            {securityFailure ? 'Session check blocked' : 'We could not reach OpenCircle'}
          </h1>
          <p className="text-muted-foreground leading-6">
            {securityFailure
              ? 'This browser session could not be verified. Check the site address and try again.'
              : 'The service may be temporarily unavailable. Your account has not been changed.'}
          </p>
        </div>
        <Button type="button" size="lg" onClick={onRetry}>
          <RefreshCw aria-hidden="true" />
          Try again
        </Button>
      </div>
    </main>
  );
}

export function AuthBootstrapProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const bootstrapError = useAuthStore((state) => state.bootstrapError);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => setupAuthInterceptors(), []);

  useEffect(() => {
    let active = true;
    const auth = useAuthStore.getState();
    auth.beginBootstrap();

    async function bootstrap() {
      try {
        await refreshAccessToken();
        if (!active) return;

        const user = await getCurrentUser();
        if (!active) return;

        queryClient.setQueryData(authQueryKeys.currentUser, user);
        useAuthStore.getState().setAuthenticated();
      } catch (error) {
        if (!active) return;

        const bootstrapFailure = classifyBootstrapError(error);
        if (bootstrapFailure) {
          useAuthStore.getState().failBootstrap(bootstrapFailure);
        } else {
          queryClient.removeQueries({ queryKey: authQueryKeys.all });
          useAuthStore.getState().setAnonymous();
        }
      }
    }

    void bootstrap();
    return () => {
      active = false;
    };
  }, [attempt, queryClient]);

  if (bootstrapError) {
    return (
      <BootstrapFailure error={bootstrapError} onRetry={() => setAttempt((value) => value + 1)} />
    );
  }

  return children;
}
