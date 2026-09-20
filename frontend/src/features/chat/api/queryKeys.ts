export const chatQueryKeys = {
  rooms: ['chat-rooms'] as const,
  hiddenRooms: ['chat-rooms', 'hidden'] as const,
  messages: (roomId: string) => ['chat-rooms', roomId, 'messages'] as const,
};
