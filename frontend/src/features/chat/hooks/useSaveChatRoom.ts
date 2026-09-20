import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveChatRoom } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useSaveChatRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: saveChatRoom,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
    },
  });
}
