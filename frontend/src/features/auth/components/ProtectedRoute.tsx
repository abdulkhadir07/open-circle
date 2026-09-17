import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { AuthLoadingScreen } from './AuthLoadingScreen';

export function ProtectedRoute() {
  const status = useAuthStore((state) => state.authStatus);
  const location = useLocation();

  if (status === 'bootstrapping') {
    return <AuthLoadingScreen />;
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
