import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sendChatAttachment } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';
import type { ChatMessage } from '../api/contracts';
import { appendChatMessage } from './chatMessageCache';

export function useSendChatAttachment(roomId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ file, caption }: { file: File; caption?: string }) =>
      sendChatAttachment({ roomId, file, caption }),
    onSuccess: (message) => {
      // Same reasoning as useSendChatMessage: the realtime broadcast will
      // also deliver this message (deduped by id), but appending it here
      // too means the sender sees it immediately.
      queryClient.setQueryData(
        chatQueryKeys.messages(roomId),
        (existing: ChatMessage[] | undefined) => appendChatMessage(existing, message),
      );
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.rooms });
    },
  });
}
