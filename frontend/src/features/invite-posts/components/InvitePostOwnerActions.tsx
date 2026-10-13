import { LoaderCircle, MoreHorizontal, Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import type { InvitePost } from '../api/contracts';
import { useDeleteInvitePost } from '../hooks/useDeleteInvitePost';
import { EditInvitePostDialog } from './EditInvitePostDialog';

export function InvitePostOwnerActions({ post }: { post: InvitePost }) {
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const deletePost = useDeleteInvitePost();

  async function handleDelete() {
    try {
      await deletePost.mutateAsync(post.id);
      setDeleteOpen(false);
    } catch {
      // The normalized mutation error is rendered inside the confirmation dialog.
    }
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label="Invite actions"
            title="Invite actions"
            className="text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:ring-ring flex size-8 shrink-0 items-center justify-center rounded-md outline-none focus-visible:ring-2"
          >
            <MoreHorizontal aria-hidden="true" className="size-4" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onSelect={() => setEditOpen(true)}>
            <Pencil aria-hidden="true" />
            Edit invite
          </DropdownMenuItem>
          <DropdownMenuItem
            className="text-destructive data-[highlighted]:text-destructive"
            onSelect={() => {
              deletePost.reset();
              setDeleteOpen(true);
            }}
          >
            <Trash2 aria-hidden="true" />
            Delete invite
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <EditInvitePostDialog post={post} open={editOpen} onOpenChange={setEditOpen} />
      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this invite?</AlertDialogTitle>
            <AlertDialogDescription>
              It will disappear from feeds. Existing conversations will stay available.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deletePost.isError ? (
            <p role="alert" className="text-destructive text-sm">
              {deletePost.error instanceof Error
                ? deletePost.error.message
                : 'Unable to delete this invite.'}
            </p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deletePost.isPending}>Cancel</AlertDialogCancel>
            <Button
              type="button"
              variant="destructive"
              disabled={deletePost.isPending}
              onClick={() => void handleDelete()}
            >
              {deletePost.isPending ? (
                <LoaderCircle aria-hidden="true" className="animate-spin" />
              ) : null}
              {deletePost.isPending ? 'Deleting' : 'Delete invite'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
