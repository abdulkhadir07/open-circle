import { EyeOff, LoaderCircle } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { useState } from 'react';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { HiddenChatRoomList } from './HiddenChatRoomList';
import { HiddenChatsPinPrompt } from './HiddenChatsPinPrompt';

export function HiddenChatsPinGate() {
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
    return <EmptyState icon={EyeOff}>No hidden chats.</EmptyState>;
  }

  if (unlockedPin) {
    return <HiddenChatRoomList pin={unlockedPin} />;
  }

  return <HiddenChatsPinPrompt onUnlock={setUnlockedPin} />;
}
