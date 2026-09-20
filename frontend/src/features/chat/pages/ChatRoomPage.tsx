import { ArrowLeft, LoaderCircle, LogOut, MoreVertical, Pin } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { MessageComposer } from '../components/MessageComposer';
import { MessageList } from '../components/MessageList';
import { useChatRoomRealtime } from '../hooks/useChatRoomRealtime';
import { useChatRooms } from '../hooks/useChatRooms';
import { useLeaveChatRoom } from '../hooks/useLeaveChatRoom';
import { useSaveChatRoom } from '../hooks/useSaveChatRoom';

export function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const rooms = useChatRooms();
  const saveRoom = useSaveChatRoom();
  const leaveRoom = useLeaveChatRoom();
  const [confirmingLeave, setConfirmingLeave] = useState(false);
  useChatRoomRealtime(roomId);

  if (rooms.isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-5 animate-spin" />
      </div>
    );
  }

  const room = rooms.data?.find((candidate) => candidate.id === roomId);

  if (!room) {
    return (
      <div className="border-border bg-card flex h-full flex-col items-center justify-center gap-3 rounded-xl border text-center">
        <p className="text-muted-foreground text-base">
          {rooms.isError ? 'Unable to load this chat.' : "This chat isn't available anymore."}
        </p>
        <Link to="/chats" className="text-primary text-base font-medium lg:hidden">
          Back to chats
        </Link>
      </div>
    );
  }

  // Keeps showing who you were talking to even after they leave the room.
  const others = room.participants.filter((p) => p.userId !== currentUser.data?.id);
  const names = others.length > 0 ? others.map((p) => p.username).join(', ') : 'Just you';
  const departedOther = others.find((p) => !p.active);
  const canSendMessages = !room.closed && others.some((p) => p.active);

  function handleLeave() {
    leaveRoom.mutate(room!.id, { onSuccess: () => navigate('/chats') });
  }

  return (
    <div className="border-border bg-card flex h-full flex-col overflow-hidden rounded-xl border">
      <div className="border-border flex shrink-0 items-center justify-between gap-3 border-b p-4">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            to="/chats"
            aria-label="Back to chats"
            className="text-muted-foreground hover:text-foreground lg:hidden"
          >
            <ArrowLeft aria-hidden="true" className="size-5" />
          </Link>
          <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
            {names[0]?.toUpperCase()}
          </span>
          <div className="min-w-0">
            <h1 className="text-foreground truncate text-base font-semibold">{names}</h1>
            {room.closed ? (
              <p className="text-sm font-medium text-yellow-700 dark:text-yellow-400">
                This chat is closed.
              </p>
            ) : null}
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {room.saved ? (
            <span className="text-primary flex items-center gap-1 text-sm font-medium">
              <Pin aria-hidden="true" className="size-3.5" />
              Saved
            </span>
          ) : null}
          {!room.closed ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label="Chat options"
                  className="text-muted-foreground hover:bg-muted hover:text-foreground flex size-8 items-center justify-center rounded-md"
                >
                  <MoreVertical aria-hidden="true" className="size-4" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                {!room.saved ? (
                  <DropdownMenuItem
                    onSelect={() => saveRoom.mutate(room.id)}
                    disabled={saveRoom.isPending}
                  >
                    <Pin aria-hidden="true" className="size-4" />
                    Save
                  </DropdownMenuItem>
                ) : null}
                <DropdownMenuItem
                  onSelect={() => setConfirmingLeave(true)}
                  disabled={leaveRoom.isPending}
                  className="text-destructive data-[highlighted]:bg-destructive/10"
                >
                  <LogOut aria-hidden="true" className="size-4" />
                  Leave chat
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : null}
        </div>
      </div>
      {leaveRoom.isError ? (
        <p role="alert" className="text-destructive px-4 pt-2 text-sm">
          {leaveRoom.error instanceof Error
            ? leaveRoom.error.message
            : 'Unable to leave that chat.'}
        </p>
      ) : null}

      <AlertDialog open={confirmingLeave} onOpenChange={setConfirmingLeave}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Leave this chat?</AlertDialogTitle>
            <AlertDialogDescription>
              Once you leave, you won't have access to this chat anymore. You won't be able to
              recover it or its message history, and you can't rejoin it later.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleLeave} disabled={leaveRoom.isPending}>
              {leaveRoom.isPending ? 'Leaving…' : 'Leave chat'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <div className="min-h-0 flex-1">
        <MessageList roomId={room.id} currentUserId={currentUser.data?.id} />
      </div>
      <div className="border-border shrink-0 border-t p-3">
        <MessageComposer
          roomId={room.id}
          disabled={!canSendMessages}
          disabledReason={
            room.closed
              ? 'This chat is closed'
              : departedOther
                ? `${departedOther.username} left this chat, so you can't send new messages`
                : undefined
          }
        />
      </div>
    </div>
  );
}
