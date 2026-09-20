import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { AuthBootstrapProvider } from '@/features/auth/AuthBootstrapProvider';
import { queryClient } from '@/lib/queryClient';
import { RealtimeProvider } from '@/lib/realtime/RealtimeProvider';

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrapProvider>
        <RealtimeProvider>{children}</RealtimeProvider>
      </AuthBootstrapProvider>
    </QueryClientProvider>
  );
}
