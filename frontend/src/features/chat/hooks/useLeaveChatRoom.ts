import { useMutation, useQueryClient } from '@tanstack/react-query';
import { leaveChatRoom } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useLeaveChatRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: leaveChatRoom,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
    },
  });
}
