import { useMutation, useQueryClient } from '@tanstack/react-query';
import { login } from '../api/authApi';
import { authQueryKeys } from '../api/queryKeys';
import { useAuthStore } from '@/stores/authStore';

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: login,
    onSuccess: ({ token, user }) => {
      queryClient.setQueryData(authQueryKeys.currentUser, user);
      useAuthStore.getState().setAuthenticated(token);
    },
  });
}
