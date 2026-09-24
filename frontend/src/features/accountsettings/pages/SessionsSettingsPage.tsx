import { SessionsList } from '../components/SessionsList';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function SessionsSettingsPage() {
  return (
    <SettingsSectionLayout title="Sessions">
      <SessionsList />
    </SettingsSectionLayout>
  );
}
