import { Eye, EyeOff, LoaderCircle } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useHiddenChatRooms } from '../hooks/useHiddenChatRooms';
import { useUnhideChatRoom } from '../hooks/useUnhideChatRoom';
import { ChatRoomRow } from './ChatRoomRow';

export function HiddenChatRoomList({ pin }: { pin: string }) {
  const currentUser = useCurrentUser();
  const rooms = useHiddenChatRooms(pin);
  const unhideRoom = useUnhideChatRoom();

  if (rooms.isLoading) {
    return (
      <LoaderCircle
        aria-hidden="true"
        className="text-muted-foreground mx-auto block size-5 animate-spin"
      />
    );
  }

  if (rooms.isError) {
    return (
      <p role="alert" className="text-destructive text-base">
        {rooms.error instanceof Error ? rooms.error.message : 'Unable to load hidden chats.'}
      </p>
    );
  }

  if (!rooms.data || rooms.data.length === 0) {
    return <EmptyState icon={EyeOff}>No hidden chats.</EmptyState>;
  }

  return (
    <div className="space-y-2">
      <ul className="space-y-3">
        {rooms.data.map((room) => (
          <ChatRoomRow
            key={room.id}
            room={room}
            currentUserId={currentUser.data?.id}
            actions={
              <button
                type="button"
                onClick={() => unhideRoom.mutate({ roomId: room.id, pin })}
                disabled={unhideRoom.isPending}
                aria-label="Unhide"
                title="Unhide"
                className="text-muted-foreground hover:bg-muted hover:text-foreground flex size-8 items-center justify-center rounded-md disabled:opacity-50"
              >
                <Eye aria-hidden="true" className="size-4" />
              </button>
            }
          />
        ))}
      </ul>
      {unhideRoom.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {unhideRoom.error instanceof Error
            ? unhideRoom.error.message
            : 'Unable to unhide that chat.'}
        </p>
      ) : null}
    </div>
  );
}
