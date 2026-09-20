import { LoaderCircle, Paperclip } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';
import type { ChatMessage } from '../api/contracts';
import { useChatMessages } from '../hooks/useChatMessages';

/** How close to the bottom (in px) still counts as "following" the conversation. */
const NEAR_BOTTOM_THRESHOLD = 150;

function formatMessageTime(createdAt: string): string {
  return new Date(createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

function SystemMessage({ text }: { text: string }) {
  return (
    <div className="flex justify-center">
      <span className="text-muted-foreground bg-muted/60 rounded-full px-3 py-1 text-xs">
        {text}
      </span>
    </div>
  );
}

function MessageBubble({ message, isOwn }: { message: ChatMessage; isOwn: boolean }) {
  if (message.type === 'PARTICIPANT_LEFT') {
    return <SystemMessage text={isOwn ? 'You left' : `${message.senderUsername} left the chat`} />;
  }

  return (
    <div className={cn('flex flex-col gap-1', isOwn ? 'items-end' : 'items-start')}>
      <div
        className={cn(
          'max-w-[75%] rounded-2xl px-3.5 py-2 text-base whitespace-pre-wrap',
          isOwn
            ? 'bg-primary text-primary-foreground rounded-br-md'
            : 'bg-muted text-foreground rounded-bl-md',
        )}
      >
        {message.type === 'ATTACHMENT' ? (
          <span className="inline-flex items-center gap-1.5 align-bottom">
            <Paperclip aria-hidden="true" className="size-3.5 shrink-0" />
            {message.attachment?.originalFilename ?? 'Attachment'}
          </span>
        ) : (
          message.body
        )}
        <span
          className={cn(
            'ml-2 text-xs tabular-nums',
            isOwn ? 'text-primary-foreground/70' : 'text-muted-foreground',
          )}
        >
          {formatMessageTime(message.createdAt)}
        </span>
      </div>
    </div>
  );
}

export function MessageList({ roomId, currentUserId }: { roomId: string; currentUserId?: string }) {
  const messages = useChatMessages(roomId);
  const containerRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const isNearBottomRef = useRef(true);
  const previousCountRef = useRef(0);

  function handleScroll() {
    const container = containerRef.current;
    if (!container) return;

    const distanceFromBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight;
    isNearBottomRef.current = distanceFromBottom < NEAR_BOTTOM_THRESHOLD;
  }

  // Auto-follows the conversation: always lands at the bottom on first load or when the
  // current user sends a message, otherwise only if they were already near the bottom —
  // so scrolling up to read history doesn't get interrupted by someone else's new message.
  useEffect(() => {
    if (!messages.data || messages.data.length === 0) return;

    const newCount = messages.data.length;
    const grew = newCount > previousCountRef.current;
    const isFirstLoad = previousCountRef.current === 0;
    const lastMessage = messages.data[newCount - 1];
    const isOwnMessage = lastMessage?.senderId === currentUserId;
    previousCountRef.current = newCount;

    if (grew && (isFirstLoad || isOwnMessage || isNearBottomRef.current)) {
      bottomRef.current?.scrollIntoView({ block: 'end' });
    }
  }, [messages.data, currentUserId]);

  if (messages.isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-5 animate-spin" />
      </div>
    );
  }

  if (messages.isError) {
    return (
      <div className="flex h-full items-center justify-center">
        <p role="alert" className="text-destructive text-base">
          {messages.error instanceof Error ? messages.error.message : 'Unable to load messages.'}
        </p>
      </div>
    );
  }

  if (!messages.data || messages.data.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-muted-foreground text-base">
          No messages yet. Say hello to get the conversation started.
        </p>
      </div>
    );
  }

  return (
    <ScrollArea ref={containerRef} onScroll={handleScroll} className="p-4">
      <div className="flex flex-col gap-4">
        {messages.data.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            isOwn={message.senderId === currentUserId}
          />
        ))}
        <div ref={bottomRef} />
      </div>
    </ScrollArea>
  );
}
