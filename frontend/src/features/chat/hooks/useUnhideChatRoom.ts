import { useMutation, useQueryClient } from '@tanstack/react-query';
import { unhideChatRoom } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useUnhideChatRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: unhideChatRoom,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.hiddenRooms });
    },
  });
}
