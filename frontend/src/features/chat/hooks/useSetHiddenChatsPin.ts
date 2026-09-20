import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { setHiddenChatsPin } from '../api/chatApi';

export function useSetHiddenChatsPin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: setHiddenChatsPin,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: authQueryKeys.currentUser });
    },
  });
}
