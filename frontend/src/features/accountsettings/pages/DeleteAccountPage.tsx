import { DeleteAccountForm } from '../components/DeleteAccountForm';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function DeleteAccountPage() {
  return (
    <SettingsSectionLayout title="Delete account">
      <DeleteAccountForm />
    </SettingsSectionLayout>
  );
}
