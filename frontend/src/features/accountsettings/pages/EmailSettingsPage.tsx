import { ChangeEmailForm } from '../components/ChangeEmailForm';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function EmailSettingsPage() {
  return (
    <SettingsSectionLayout title="Email">
      <ChangeEmailForm />
    </SettingsSectionLayout>
  );
}
