import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { changePassword, type ChangePasswordPayload } from '../api/accountSettingsApi';
import { accountSettingsQueryKeys } from '../api/queryKeys';

export function useChangePassword() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ChangePasswordPayload) => changePassword(payload),
    onSuccess: (token) => {
      useAuthStore.getState().setAccessToken(token);
      void queryClient.invalidateQueries({ queryKey: accountSettingsQueryKeys.sessions });
    },
  });
}
