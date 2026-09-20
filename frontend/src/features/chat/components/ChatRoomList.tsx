import { EyeOff, LoaderCircle } from 'lucide-react';
import { useState } from 'react';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useChatRooms } from '../hooks/useChatRooms';
import { useHideChatRoom } from '../hooks/useHideChatRoom';
import { ChatRoomRow } from './ChatRoomRow';
import { SetHiddenChatsPinForm } from './SetHiddenChatsPinForm';

export function ChatRoomList({ selectedRoomId }: { selectedRoomId?: string }) {
  const currentUser = useCurrentUser();
  const rooms = useChatRooms();
  const hideRoom = useHideChatRoom();
  const [pinSetupRoomId, setPinSetupRoomId] = useState<string | null>(null);

  function handleHide(roomId: string) {
    if (currentUser.data?.hasHiddenChatsPin) {
      hideRoom.mutate(roomId);
    } else {
      setPinSetupRoomId(roomId);
    }
  }

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
        {rooms.error instanceof Error ? rooms.error.message : 'Unable to load your chats.'}
      </p>
    );
  }

  if (!rooms.data || rooms.data.length === 0) {
    return (
      <p className="text-muted-foreground text-base">
        No conversations yet. Accepting or being accepted into an invite starts one.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      <ul className="space-y-0.5">
        {rooms.data.map((room) => (
          <ChatRoomRow
            key={room.id}
            room={room}
            currentUserId={currentUser.data?.id}
            isSelected={room.id === selectedRoomId}
            actions={
              !room.closed ? (
                <button
                  type="button"
                  onClick={() => handleHide(room.id)}
                  disabled={hideRoom.isPending}
                  aria-label="Hide"
                  title="Hide"
                  className="text-muted-foreground hover:bg-background hover:text-foreground flex size-8 items-center justify-center rounded-md disabled:opacity-50"
                >
                  <EyeOff aria-hidden="true" className="size-4" />
                </button>
              ) : null
            }
          />
        ))}
      </ul>
      {hideRoom.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {hideRoom.error instanceof Error ? hideRoom.error.message : 'Unable to hide that chat.'}
        </p>
      ) : null}

      <Dialog
        open={pinSetupRoomId !== null}
        onOpenChange={(open) => !open && setPinSetupRoomId(null)}
      >
        <DialogContent>
          <DialogTitle className="sr-only">Hiding a chat requires a Hidden chats PIN</DialogTitle>
          <SetHiddenChatsPinForm
            onSuccess={() => {
              if (pinSetupRoomId) hideRoom.mutate(pinSetupRoomId);
              setPinSetupRoomId(null);
            }}
            onCancel={() => setPinSetupRoomId(null)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
