import { AuthLoadingScreen } from '@/features/auth/components/AuthLoadingScreen';
import { useAuthStore } from '@/stores/authStore';
import { AppLayout } from '@/components/layout/AppLayout';
import { HomePage } from './HomePage';
import { LandingPage } from './LandingPage';

/**
 * `/` renders different content depending on auth status rather than
 * redirecting anonymous visitors away, unlike every other protected route.
 */
export function RootPage() {
  const status = useAuthStore((state) => state.authStatus);

  if (status === 'bootstrapping') {
    return <AuthLoadingScreen />;
  }

  if (status === 'authenticated') {
    return (
      <AppLayout>
        <HomePage />
      </AppLayout>
    );
  }

  return <LandingPage />;
}
