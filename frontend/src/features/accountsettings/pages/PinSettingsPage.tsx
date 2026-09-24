import { HiddenChatsPinForm } from '../components/HiddenChatsPinForm';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function PinSettingsPage() {
  return (
    <SettingsSectionLayout title="Hidden chats PIN">
      <HiddenChatsPinForm />
    </SettingsSectionLayout>
  );
}
