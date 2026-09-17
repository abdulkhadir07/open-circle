import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { AuthLoadingScreen } from './AuthLoadingScreen';

export function PublicOnlyRoute() {
  const status = useAuthStore((state) => state.authStatus);

  if (status === 'bootstrapping') {
    return <AuthLoadingScreen />;
  }
  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
