import { Heart, MessageCircle, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Avatar } from '@/components/ui/avatar';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Card } from '@/components/ui/card';
import { ProfileAvatarLink, ProfileLink } from '@/features/profile/components/ProfileLink';
import { cn } from '@/lib/utils';
import type { Banter } from '../api/contracts';
import { useDeleteBanter } from '../hooks/useDeleteBanter';
import { useToggleBanterLike } from '../hooks/useToggleBanterLike';
import { formatShortAgo } from '../lib/formatShortAgo';
import { BanterThread } from './BanterThread';

/** "Reply" when there are none yet, otherwise the count in words, as openSFSU shows it. */
function replyLabel(count: number) {
  if (count === 0) return 'Reply';
  return `${count} ${count === 1 ? 'reply' : 'replies'}`;
}

export function BanterCard({ banter }: { banter: Banter }) {
  const [showReplies, setShowReplies] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const toggleLike = useToggleBanterLike();
  const deleteBanter = useDeleteBanter();

  return (
    <Card as="article" className="animate-fade-up hover:border-primary/40 p-4 transition">
      <div className="flex gap-3">
        <ProfileAvatarLink userId={banter.authorId}>
          <Avatar name={banter.authorUsername} profileImage={banter.authorProfileImage} />
        </ProfileAvatarLink>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5 text-sm">
            <span className="min-w-0 truncate font-semibold">
              <ProfileLink userId={banter.authorId}>{banter.authorUsername}</ProfileLink>
            </span>
            <span className="text-muted-foreground shrink-0">
              · {formatShortAgo(banter.createdAt)}
            </span>
            {banter.mine ? (
              <button
                type="button"
                aria-label="Delete banter"
                onClick={() => setConfirmingDelete(true)}
                className="text-muted-foreground hover:text-destructive -my-1 ml-auto cursor-pointer rounded-md p-1.5"
              >
                <Trash2 aria-hidden="true" className="size-4" />
              </button>
            ) : null}
          </div>

          <p className="mt-1 text-[15px] leading-relaxed break-words whitespace-pre-wrap">
            {banter.content}
          </p>

          <div className="text-muted-foreground mt-2 flex items-center gap-4 text-sm">
            <button
              type="button"
              aria-pressed={banter.likedByMe}
              aria-label={banter.likedByMe ? 'Unlike' : 'Like'}
              onClick={() => toggleLike.mutate({ banterId: banter.id, like: !banter.likedByMe })}
              className={cn(
                'flex cursor-pointer items-center gap-1 transition active:scale-90',
                banter.likedByMe ? 'text-primary' : 'hover:text-primary',
              )}
            >
              <Heart
                aria-hidden="true"
                className={cn('size-4 transition', banter.likedByMe && 'fill-primary')}
              />
              {banter.likeCount > 0 ? (
                <span className="tabular-nums">{banter.likeCount}</span>
              ) : null}
            </button>
            <button
              type="button"
              aria-expanded={showReplies}
              onClick={() => setShowReplies((open) => !open)}
              className={cn(
                'flex cursor-pointer items-center gap-1 transition',
                showReplies ? 'text-primary' : 'hover:text-primary',
              )}
            >
              <MessageCircle aria-hidden="true" className="size-4" />
              <span>{replyLabel(banter.replyCount)}</span>
            </button>
          </div>

          {showReplies ? (
            <BanterThread banterId={banter.id} focusInput={banter.replyCount === 0} />
          ) : null}
        </div>
      </div>

      <AlertDialog open={confirmingDelete} onOpenChange={setConfirmingDelete}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this banter?</AlertDialogTitle>
            <AlertDialogDescription>
              It will disappear for everyone on your campus, along with its replies and likes.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => deleteBanter.mutate(banter.id)}
              disabled={deleteBanter.isPending}
            >
              {deleteBanter.isPending ? 'Deleting…' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
