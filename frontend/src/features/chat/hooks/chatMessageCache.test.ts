import { describe, expect, it } from 'vitest';
import { chatMessage } from '@/test/mocks/fixtures';
import { appendChatMessage } from './chatMessageCache';

describe('appendChatMessage', () => {
  it('starts a new list when there is no existing cache', () => {
    expect(appendChatMessage(undefined, chatMessage)).toEqual([chatMessage]);
  });

  it('appends a new message onto the existing list', () => {
    const other = { ...chatMessage, id: 'other-id', body: 'Second message' };

    expect(appendChatMessage([chatMessage], other)).toEqual([chatMessage, other]);
  });

  it('does not duplicate a message with the same id', () => {
    expect(appendChatMessage([chatMessage], chatMessage)).toEqual([chatMessage]);
  });
});
