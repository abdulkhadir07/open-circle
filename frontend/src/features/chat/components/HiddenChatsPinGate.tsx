import { LoaderCircle } from 'lucide-react';
import { useState } from 'react';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { HiddenChatRoomList } from './HiddenChatRoomList';
import { HiddenChatsPinPrompt } from './HiddenChatsPinPrompt';

export function HiddenChatsPinGate({ selectedRoomId }: { selectedRoomId?: string }) {
  const currentUser = useCurrentUser();
  const [unlockedPin, setUnlockedPin] = useState<string | null>(null);

  if (currentUser.isLoading) {
    return (
      <LoaderCircle
        aria-hidden="true"
        className="text-muted-foreground mx-auto block size-5 animate-spin"
      />
    );
  }

  if (!currentUser.data?.hasHiddenChatsPin) {
    return <p className="text-muted-foreground text-base">No hidden chats.</p>;
  }

  if (unlockedPin) {
    return <HiddenChatRoomList pin={unlockedPin} selectedRoomId={selectedRoomId} />;
  }

  return <HiddenChatsPinPrompt onUnlock={setUnlockedPin} />;
}
