import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { revokeAllSessions } from '../api/accountSettingsApi';

export function useRevokeAllSessions() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: revokeAllSessions,
    onSuccess: () => {
      queryClient.clear();
      useAuthStore.getState().setAnonymous();
    },
  });
}
