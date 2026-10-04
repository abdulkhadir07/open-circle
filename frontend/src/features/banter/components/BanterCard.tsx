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
import { formatRelativeTime } from '@/features/ratings/lib/formatRelativeTime';
import { cn } from '@/lib/utils';
import type { Banter } from '../api/contracts';
import { useDeleteBanter } from '../hooks/useDeleteBanter';
import { useToggleBanterLike } from '../hooks/useToggleBanterLike';
import { BanterThread } from './BanterThread';

export function BanterCard({ banter }: { banter: Banter }) {
  const [showReplies, setShowReplies] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const toggleLike = useToggleBanterLike();
  const deleteBanter = useDeleteBanter();

  return (
    <Card as="article" className="animate-fade-up">
      <div className="flex items-center gap-3">
        <ProfileAvatarLink userId={banter.authorId}>
          <Avatar name={banter.authorUsername} profileImage={banter.authorProfileImage} />
        </ProfileAvatarLink>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">
            <ProfileLink userId={banter.authorId}>{banter.authorUsername}</ProfileLink>
          </p>
          <p className="text-muted-foreground text-xs">{formatRelativeTime(banter.createdAt)}</p>
        </div>
        {banter.mine ? (
          <button
            type="button"
            aria-label="Delete banter"
            onClick={() => setConfirmingDelete(true)}
            className="text-muted-foreground hover:text-destructive cursor-pointer rounded-md p-1.5"
          >
            <Trash2 aria-hidden="true" className="size-4" />
          </button>
        ) : null}
      </div>

      <p className="mt-3 text-[15px] leading-relaxed whitespace-pre-wrap">{banter.content}</p>

      <div className="mt-3 flex items-center gap-4 text-sm">
        <button
          type="button"
          aria-pressed={banter.likedByMe}
          aria-label={banter.likedByMe ? 'Unlike' : 'Like'}
          onClick={() => toggleLike.mutate({ banterId: banter.id, like: !banter.likedByMe })}
          className={cn(
            'flex cursor-pointer items-center gap-1.5 transition active:scale-95',
            banter.likedByMe ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <Heart aria-hidden="true" className={cn('size-4', banter.likedByMe && 'fill-primary')} />
          <span className="tabular-nums">{banter.likeCount}</span>
        </button>
        <button
          type="button"
          aria-expanded={showReplies}
          aria-label={`${banter.replyCount} ${banter.replyCount === 1 ? 'reply' : 'replies'}`}
          onClick={() => setShowReplies((open) => !open)}
          className={cn(
            'flex cursor-pointer items-center gap-1.5 transition active:scale-95',
            showReplies ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <MessageCircle aria-hidden="true" className="size-4" />
          <span className="tabular-nums">{banter.replyCount}</span>
        </button>
      </div>

      {showReplies ? <BanterThread banterId={banter.id} /> : null}

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
