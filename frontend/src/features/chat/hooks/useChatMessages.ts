import { useQuery } from '@tanstack/react-query';
import { getChatMessages } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useChatMessages(roomId: string) {
  return useQuery({
    queryKey: chatQueryKeys.messages(roomId),
    queryFn: () => getChatMessages(roomId),
  });
}
