import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getHiddenChatRooms } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useVerifyHiddenChatsPin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (pin: string) => getHiddenChatRooms(pin),
    onSuccess: (rooms) => {
      queryClient.setQueryData(chatQueryKeys.hiddenRooms, rooms);
    },
  });
}
