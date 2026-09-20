import { useQuery } from '@tanstack/react-query';
import { getHiddenChatRooms } from '../api/chatApi';
import { chatQueryKeys } from '../api/queryKeys';

export function useHiddenChatRooms(pin: string, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: chatQueryKeys.hiddenRooms,
    queryFn: () => getHiddenChatRooms(pin),
    enabled: options.enabled ?? true,
  });
}
