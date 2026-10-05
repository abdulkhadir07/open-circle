import { SafetyBlockedNotice } from '@/features/ai/components/SafetyBlockedNotice';
import { isContentBlockedError } from '@/lib/api/errors';
import { LoaderCircle, Paperclip, Send } from 'lucide-react';
import { type ChangeEvent, type KeyboardEvent, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useSendChatAttachment } from '../hooks/useSendChatAttachment';
import { useSendChatMessage } from '../hooks/useSendChatMessage';
import { validateAttachment } from '../lib/validateAttachment';

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
  const [attachmentError, setAttachmentError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const sendMessage = useSendChatMessage(roomId);
  const sendAttachment = useSendChatAttachment(roomId);
  const isPending = sendMessage.isPending || sendAttachment.isPending;

  function submit() {
    const trimmed = body.trim();
    if (!trimmed || isPending) return;

    sendMessage.mutate(trimmed, { onSuccess: () => setBody('') });
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || isPending) return;

    const validationMessage = validateAttachment(file);
    if (validationMessage) {
      setAttachmentError(validationMessage);
      return;
    }

    setAttachmentError(null);
    const trimmed = body.trim();
    sendAttachment.mutate(
      { file, caption: trimmed || undefined },
      { onSuccess: () => setBody('') },
    );
  }

  // A message Safety Guardian refused gets a friendly notice instead of plain error text.
  const blockedError = [sendMessage.error, sendAttachment.error].find(isContentBlockedError);

  const errorMessage =
    attachmentError ??
    (sendMessage.isError
      ? sendMessage.error instanceof Error
        ? sendMessage.error.message
        : 'Unable to send that message.'
      : null) ??
    (sendAttachment.isError
      ? sendAttachment.error instanceof Error
        ? sendAttachment.error.message
        : 'Unable to send that attachment.'
      : null);

  return (
    <div>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        className="flex items-end gap-2"
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,application/pdf"
          onChange={handleFileSelected}
          disabled={disabled || isPending}
          className="hidden"
        />
        <Button
          type="button"
          variant="outline"
          size="icon"
          disabled={disabled || isPending}
          aria-label="Attach a file"
          onClick={() => fileInputRef.current?.click()}
        >
          {sendAttachment.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <Paperclip aria-hidden="true" />
          )}
        </Button>
        <Textarea
          value={body}
          onChange={(event) => setBody(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled || isPending}
          placeholder={disabled ? disabledReason : 'Write a message…'}
          aria-label="Message"
          rows={1}
          className="min-h-10 flex-1 resize-none py-2.5"
        />
        <Button
          type="submit"
          size="icon"
          disabled={disabled || isPending || body.trim().length === 0}
          aria-label="Send message"
        >
          {sendMessage.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <Send aria-hidden="true" />
          )}
        </Button>
      </form>
      {blockedError ? (
        <div className="mt-1.5">
          <SafetyBlockedNotice message={blockedError.message} />
        </div>
      ) : errorMessage ? (
        <p role="alert" className="text-destructive mt-1.5 text-sm">
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
