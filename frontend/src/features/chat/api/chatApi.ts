import { apiClient } from '@/lib/api/client';
import { normalizeApiError } from '@/lib/api/errors';
import {
  parseChatMessage,
  parseChatMessageList,
  parseChatRoom,
  parseChatRoomList,
} from './contracts';

async function normalizeFailure<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function getChatRooms() {
  const response = await normalizeFailure(apiClient.get('/chat-rooms'));
  return parseChatRoomList(response.data);
}

export async function getHiddenChatRooms(pin: string) {
  const response = await normalizeFailure(
    apiClient.get('/chat-rooms/hidden', {
      headers: { 'X-Hidden-Chats-Pin': pin },
      // A wrong PIN legitimately returns 401, but that's unrelated to the access
      // token — skip the auth interceptor's refresh-and-retry so it doesn't spend
      // a second PIN attempt on the retry or misread this as a dead session.
      skipUnauthorizedRetry: true,
    }),
  );
  return parseChatRoomList(response.data);
}

export async function getChatMessages(roomId: string) {
  const response = await normalizeFailure(apiClient.get(`/chat-rooms/${roomId}/messages`));
  return parseChatMessageList(response.data);
}

export async function sendChatMessage({ roomId, body }: { roomId: string; body: string }) {
  const response = await normalizeFailure(
    apiClient.post(`/chat-rooms/${roomId}/messages`, { body }),
  );
  return parseChatMessage(response.data);
}

export async function saveChatRoom(roomId: string) {
  const response = await normalizeFailure(apiClient.patch(`/chat-rooms/${roomId}/save`));
  return parseChatRoom(response.data);
}

export async function leaveChatRoom(roomId: string) {
  const response = await normalizeFailure(apiClient.patch(`/chat-rooms/${roomId}/leave`));
  return parseChatRoom(response.data);
}

export async function hideChatRoom(roomId: string) {
  const response = await normalizeFailure(apiClient.patch(`/chat-rooms/${roomId}/hide`));
  return parseChatRoom(response.data);
}

export async function unhideChatRoom({ roomId, pin }: { roomId: string; pin: string }) {
  const response = await normalizeFailure(
    apiClient.patch(
      `/chat-rooms/${roomId}/unhide`,
      {},
      { headers: { 'X-Hidden-Chats-Pin': pin }, skipUnauthorizedRetry: true },
    ),
  );
  return parseChatRoom(response.data);
}

export async function setHiddenChatsPin({
  pin,
  currentPassword,
}: {
  pin: string;
  currentPassword?: string;
}) {
  await normalizeFailure(apiClient.put('/users/me/hidden-chats-pin', { pin, currentPassword }));
}
