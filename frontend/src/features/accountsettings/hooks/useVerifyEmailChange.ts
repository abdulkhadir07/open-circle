import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { verifyEmailChange } from '../api/accountSettingsApi';
import { accountSettingsQueryKeys } from '../api/queryKeys';

export function useVerifyEmailChange() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) => verifyEmailChange(code),
    onSuccess: (token) => {
      useAuthStore.getState().setAccessToken(token);
      void queryClient.invalidateQueries({ queryKey: authQueryKeys.currentUser });
      void queryClient.invalidateQueries({ queryKey: accountSettingsQueryKeys.sessions });
    },
  });
}
