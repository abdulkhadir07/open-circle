import { PersonalInfoCard } from '../components/PersonalInfoCard';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

export function PersonalInfoSettingsPage() {
  return (
    <SettingsSectionLayout title="Personal info">
      <PersonalInfoCard />
    </SettingsSectionLayout>
  );
}
