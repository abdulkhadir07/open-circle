import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { stompClient } from '@/lib/realtime/stompClient';
import { parseChatMessage, type ChatMessage } from '../api/contracts';
import { chatQueryKeys } from '../api/queryKeys';
import { appendChatMessage } from './chatMessageCache';

export function useChatRoomRealtime(roomId: string | undefined) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!roomId) return;

    const unsubscribeTopic = stompClient.subscribe(`/topic/chat-rooms/${roomId}`, (message) => {
      try {
        const parsed = parseChatMessage(JSON.parse(message.body));
        queryClient.setQueryData(
          chatQueryKeys.messages(roomId),
          (existing: ChatMessage[] | undefined) => appendChatMessage(existing, parsed),
        );
      } catch (error) {
        console.error('Failed to process realtime chat message', error);
      }
    });

    const unsubscribeReconnect = stompClient.onReconnect(() => {
      void queryClient.invalidateQueries({ queryKey: chatQueryKeys.messages(roomId) });
    });

    return () => {
      unsubscribeTopic();
      unsubscribeReconnect();
    };
  }, [roomId, queryClient]);
}
