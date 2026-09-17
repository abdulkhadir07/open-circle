import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../api/authApi';
import { authQueryKeys } from '../api/queryKeys';
import { useAuthStore } from '@/stores/authStore';

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((state) => state.authStatus === 'authenticated');

  return useQuery({
    queryKey: authQueryKeys.currentUser,
    queryFn: getCurrentUser,
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}
