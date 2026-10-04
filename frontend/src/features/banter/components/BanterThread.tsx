import { LoaderCircle, Send, Trash2 } from 'lucide-react';
import { useState, type KeyboardEvent } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { AnimatedError } from '@/features/auth/components/AnimatedError';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { formatRelativeTime } from '@/features/ratings/lib/formatRelativeTime';
import type { BanterReply } from '../api/contracts';
import { useBanterReplies } from '../hooks/useBanterReplies';
import { useCreateBanterReply } from '../hooks/useCreateBanterReply';
import { useDeleteBanterReply } from '../hooks/useDeleteBanterReply';
import { BANTER_MAX_LENGTH } from './BanterComposer';

function ReplyRow({ reply }: { reply: BanterReply }) {
  const deleteReply = useDeleteBanterReply();

  return (
    <li className="flex gap-2.5">
      <ProfileAvatarLink userId={reply.authorId}>
        <Avatar
          name={reply.authorUsername}
          profileImage={reply.authorProfileImage}
          className="size-7 text-xs"
        />
      </ProfileAvatarLink>
      <div className="bg-muted/60 min-w-0 flex-1 rounded-2xl px-3 py-2">
        <div className="flex items-center gap-2 text-xs">
          <span className="truncate font-semibold">
            <ProfileLink userId={reply.authorId}>{reply.authorUsername}</ProfileLink>
          </span>
          <span className="text-muted-foreground shrink-0">
            {formatRelativeTime(reply.createdAt)}
          </span>
          {reply.mine ? (
            <button
              type="button"
              aria-label="Delete reply"
              disabled={deleteReply.isPending}
              onClick={() => deleteReply.mutate({ banterId: reply.banterId, replyId: reply.id })}
              className="text-muted-foreground hover:text-destructive ml-auto cursor-pointer rounded p-0.5"
            >
              <Trash2 aria-hidden="true" className="size-3.5" />
            </button>
          ) : null}
        </div>
        <p className="mt-0.5 text-sm whitespace-pre-wrap">{reply.content}</p>
      </div>
    </li>
  );
}

export function BanterThread({ banterId }: { banterId: string }) {
  const replies = useBanterReplies(banterId, true);
  const createReply = useCreateBanterReply();
  const [text, setText] = useState('');
  const trimmed = text.trim();

  function submit() {
    if (!trimmed || createReply.isPending) return;
    createReply.mutate({ banterId, content: trimmed }, { onSuccess: () => setText('') });
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      submit();
    }
  }

  const errorMessage = createReply.isError
    ? createReply.error instanceof Error
      ? createReply.error.message
      : 'Unable to post your reply.'
    : undefined;

  return (
    <div className="mt-3 border-t pt-3">
      {replies.isLoading ? (
        <LoaderCircle
          aria-hidden="true"
          className="text-muted-foreground mx-auto block size-4 animate-spin"
        />
      ) : replies.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {replies.error instanceof Error ? replies.error.message : 'Unable to load replies.'}
        </p>
      ) : replies.data && replies.data.length > 0 ? (
        <ul className="mb-3 space-y-2">
          {replies.data.map((reply) => (
            <ReplyRow key={reply.id} reply={reply} />
          ))}
        </ul>
      ) : null}

      <div className="flex items-center gap-2">
        <label htmlFor={`reply-${banterId}`} className="sr-only">
          Write a reply
        </label>
        <input
          id={`reply-${banterId}`}
          value={text}
          maxLength={BANTER_MAX_LENGTH}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Write a reply..."
          className="bg-background focus:ring-primary/40 min-w-0 flex-1 rounded-full border px-3.5 py-1.5 text-sm outline-none focus:ring-2"
        />
        <Button
          type="button"
          size="icon-sm"
          aria-label="Send reply"
          onClick={submit}
          disabled={!trimmed || createReply.isPending}
        >
          {createReply.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <Send aria-hidden="true" />
          )}
        </Button>
      </div>
      <div className="mt-1.5">
        <AnimatedError message={errorMessage} />
      </div>
    </div>
  );
}
