import { ChangePasswordForm } from '../components/ChangePasswordForm';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function PasswordSettingsPage() {
  return (
    <SettingsSectionLayout title="Password">
      <ChangePasswordForm />
    </SettingsSectionLayout>
  );
}
