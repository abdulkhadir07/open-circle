import { useQuery } from '@tanstack/react-query';
import { getChatRooms } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useChatRooms() {
  return useQuery({
    queryKey: chatQueryKeys.rooms,
    queryFn: getChatRooms,
  });
}
