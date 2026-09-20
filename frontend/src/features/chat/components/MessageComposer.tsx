import { LoaderCircle, Send } from 'lucide-react';
import { type KeyboardEvent, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useSendChatMessage } from '../hooks/useSendChatMessage';

export function MessageComposer({
  roomId,
  disabled = false,
  disabledReason = 'This chat is closed',
}: {
  roomId: string;
  disabled?: boolean;
  disabledReason?: string;
}) {
  const [body, setBody] = useState('');
  const sendMessage = useSendChatMessage(roomId);

  function submit() {
    const trimmed = body.trim();
    if (!trimmed || sendMessage.isPending) return;

    sendMessage.mutate(trimmed, { onSuccess: () => setBody('') });
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  return (
    <div>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        className="flex items-end gap-2"
      >
        <Textarea
          value={body}
          onChange={(event) => setBody(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled || sendMessage.isPending}
          placeholder={disabled ? disabledReason : 'Write a message…'}
          aria-label="Message"
          rows={1}
          className="min-h-10 flex-1 resize-none py-2.5"
        />
        <Button
          type="submit"
          size="icon"
          disabled={disabled || sendMessage.isPending || body.trim().length === 0}
          aria-label="Send message"
        >
          {sendMessage.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <Send aria-hidden="true" />
          )}
        </Button>
      </form>
      {sendMessage.isError ? (
        <p role="alert" className="text-destructive mt-1.5 text-sm">
          {sendMessage.error instanceof Error
            ? sendMessage.error.message
            : 'Unable to send that message.'}
        </p>
      ) : null}
    </div>
  );
}
