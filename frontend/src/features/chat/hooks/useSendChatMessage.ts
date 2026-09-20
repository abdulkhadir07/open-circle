import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sendChatMessage } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';
import type { ChatMessage } from '../api/contracts';
import { appendChatMessage } from './chatMessageCache';

export function useSendChatMessage(roomId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) => sendChatMessage({ roomId, body }),
    onSuccess: (message) => {
      // The realtime broadcast will also deliver this message (deduped by id) —
      // appended here too so the sender sees it immediately, without waiting
      // for the round trip through the broker.
      queryClient.setQueryData(
        chatQueryKeys.messages(roomId),
        (existing: ChatMessage[] | undefined) => appendChatMessage(existing, message),
      );
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
    },
  });
}
