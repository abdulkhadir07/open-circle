import { useMutation, useQueryClient } from '@tanstack/react-query';
import { verifyEmail } from '../api/authApi';
import { authQueryKeys } from '../api/queryKeys';
import { useAuthStore } from '@/stores/authStore';

export function useVerifyEmail() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: verifyEmail,
    onSuccess: ({ token, user }) => {
      queryClient.setQueryData(authQueryKeys.currentUser, user);
      useAuthStore.getState().setAuthenticated(token);
    },
  });
}
