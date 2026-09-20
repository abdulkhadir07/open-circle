import type { ChatMessage } from '../api/contracts';

export function appendChatMessage(
  existing: ChatMessage[] | undefined,
  incoming: ChatMessage,
): ChatMessage[] {
  if (!existing) return [incoming];
  if (existing.some((message) => message.id === incoming.id)) return existing;
  return [...existing, incoming];
}
