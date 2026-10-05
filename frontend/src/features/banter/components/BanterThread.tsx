import { LoaderCircle, Send, Trash2 } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { ContentError } from '@/features/ai/components/ContentError';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import type { BanterReply } from '../api/contracts';
import { useBanterReplies } from '../hooks/useBanterReplies';
import { useCreateBanterReply } from '../hooks/useCreateBanterReply';
import { useDeleteBanterReply } from '../hooks/useDeleteBanterReply';
import { formatShortAgo } from '../lib/formatShortAgo';
import { BANTER_MAX_LENGTH } from './BanterComposer';

function ReplyRow({ reply }: { reply: BanterReply }) {
  const deleteReply = useDeleteBanterReply();

  return (
    <li className="flex gap-2">
      <ProfileAvatarLink userId={reply.authorId}>
        <Avatar
          name={reply.authorUsername}
          profileImage={reply.authorProfileImage}
          className="size-6 text-[10px]"
        />
      </ProfileAvatarLink>
      <div className="bg-muted/60 min-w-0 rounded-xl px-3 py-1.5 text-sm">
        <div className="flex items-center gap-1.5">
          <span className="truncate font-semibold">
            <ProfileLink userId={reply.authorId}>{reply.authorUsername}</ProfileLink>
          </span>
          <span className="text-muted-foreground shrink-0 text-xs">
            {formatShortAgo(reply.createdAt)}
          </span>
          {reply.mine ? (
            <button
              type="button"
              aria-label="Delete reply"
              disabled={deleteReply.isPending}
              onClick={() => deleteReply.mutate({ banterId: reply.banterId, replyId: reply.id })}
              className="text-muted-foreground hover:text-destructive ml-1 cursor-pointer rounded p-0.5"
            >
              <Trash2 aria-hidden="true" className="size-3.5" />
            </button>
          ) : null}
        </div>
        <p className="break-words whitespace-pre-wrap">{reply.content}</p>
      </div>
    </li>
  );
}

export function BanterThread({
  banterId,
  focusInput = false,
}: {
  banterId: string;
  /** Put the cursor in the reply box straight away (when nobody has replied yet). */
  focusInput?: boolean;
}) {
  const replies = useBanterReplies(banterId, true);
  const createReply = useCreateBanterReply();
  const [text, setText] = useState('');
  const trimmed = text.trim();

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!trimmed || createReply.isPending) return;
    createReply.mutate({ banterId, content: trimmed }, { onSuccess: () => setText('') });
  }

  return (
    <div className="animate-fade-up border-primary/20 mt-3 space-y-2 border-l-2 pl-3">
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
        <ul className="space-y-2">
          {replies.data.map((reply) => (
            <ReplyRow key={reply.id} reply={reply} />
          ))}
        </ul>
      ) : null}

      <form onSubmit={submit} className="flex gap-2 pt-1">
        <label htmlFor={`reply-${banterId}`} className="sr-only">
          Write a reply
        </label>
        <input
          id={`reply-${banterId}`}
          value={text}
          maxLength={BANTER_MAX_LENGTH}
          onChange={(event) => setText(event.target.value)}
          // eslint-disable-next-line jsx-a11y/no-autofocus
          autoFocus={focusInput}
          placeholder="Write a reply"
          className="bg-card focus:ring-primary/40 min-w-0 flex-1 rounded-full border px-3 py-1.5 text-sm outline-none focus:ring-2"
        />
        <button
          type="submit"
          aria-label="Send reply"
          disabled={!trimmed || createReply.isPending}
          className="bg-primary text-primary-foreground cursor-pointer rounded-full p-2 transition active:scale-90 disabled:opacity-50"
        >
          {createReply.isPending ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : (
            <Send aria-hidden="true" className="size-4" />
          )}
        </button>
      </form>
      <ContentError error={createReply.error} fallback="Unable to post your reply." />
    </div>
  );
}
