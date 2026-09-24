import { useMutation, useQueryClient } from '@tanstack/react-query';
import { revokeSession } from '../api/accountSettingsApi';
import { accountSettingsQueryKeys } from '../api/queryKeys';

export function useRevokeSession() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sessionId: string) => revokeSession(sessionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: accountSettingsQueryKeys.sessions });
    },
  });
}
