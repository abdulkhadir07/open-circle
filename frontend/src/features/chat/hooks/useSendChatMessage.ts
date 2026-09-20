import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sendChatMessage } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useSendChatMessage(roomId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) => sendChatMessage({ roomId, body }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.messages(roomId) });
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
    },
  });
}
