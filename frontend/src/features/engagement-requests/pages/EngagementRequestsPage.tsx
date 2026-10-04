import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/components/ui/page-header';
import { Tabs } from '@/components/ui/tabs';
import { ReceivedRequestsList } from '../components/ReceivedRequestsList';
import { SentRequestsList } from '../components/SentRequestsList';

const TAB_ITEMS = [
  { key: 'received', to: '/requests?tab=received', label: 'Received' },
  { key: 'sent', to: '/requests?tab=sent', label: 'Sent' },
];

export function EngagementRequestsPage() {
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') === 'sent' ? 'sent' : 'received';

  return (
    <div>
      <PageHeader
        title="Requests"
        sub="People who want to join you, and invites you asked to join."
      />
      <Tabs items={TAB_ITEMS} active={tab} />
      {tab === 'received' ? <ReceivedRequestsList /> : <SentRequestsList />}
    </div>
  );
}
