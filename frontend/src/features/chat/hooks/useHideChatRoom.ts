import { useMutation, useQueryClient } from '@tanstack/react-query';
import { hideChatRoom } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useHideChatRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: hideChatRoom,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.hiddenRooms });
    },
  });
}
