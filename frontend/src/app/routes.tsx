import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { NotFoundPage } from '@/components/layout/NotFoundPage';
import { AuthLoadingScreen } from '@/features/auth/components/AuthLoadingScreen';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { PublicOnlyRoute } from '@/features/auth/components/PublicOnlyRoute';

const RootPage = lazy(() => import('./RootPage').then((module) => ({ default: module.RootPage })));
const LoginPage = lazy(() =>
  import('@/features/auth/pages/LoginPage').then((module) => ({ default: module.LoginPage })),
);
const SignupPage = lazy(() =>
  import('@/features/auth/pages/SignupPage').then((module) => ({ default: module.SignupPage })),
);
const VerifyEmailPage = lazy(() =>
  import('@/features/auth/pages/VerifyEmailPage').then((module) => ({
    default: module.VerifyEmailPage,
  })),
);
const EngagementRequestsPage = lazy(() =>
  import('@/features/engagement-requests/pages/EngagementRequestsPage').then((module) => ({
    default: module.EngagementRequestsPage,
  })),
);

export function AppRoutes() {
  return (
    <Suspense fallback={<AuthLoadingScreen />}>
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
        </Route>
        <Route path="/" element={<RootPage />} />
        <Route element={<ProtectedRoute />}>
          <Route
            path="/requests"
            element={
              <AppLayout>
                <EngagementRequestsPage />
              </AppLayout>
            }
          />
        </Route>
        <Route
          path="*"
          element={
            <AppLayout>
              <NotFoundPage />
            </AppLayout>
          }
        />
      </Routes>
    </Suspense>
  );
}
