import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/components/ui/page-header';
import { Tabs } from '@/components/ui/tabs';
import { ChatRoomList } from '../components/ChatRoomList';
import { HiddenChatsPinGate } from '../components/HiddenChatsPinGate';

const TAB_ITEMS = [
  { key: 'active', to: '/chats', label: 'Chats' },
  { key: 'hidden', to: '/chats?tab=hidden', label: 'Hidden' },
];

export function ChatRoomsPage() {
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') === 'hidden' ? 'hidden' : 'active';

  return (
    <div>
      <PageHeader title="Chats" sub="Plan the details with your group." />
      <Tabs items={TAB_ITEMS} active={tab} />
      {tab === 'active' ? <ChatRoomList /> : <HiddenChatsPinGate />}
    </div>
  );
}
