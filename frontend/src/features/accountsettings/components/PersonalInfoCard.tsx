import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';

function formatDateOfBirth(iso: string | undefined): string {
  if (!iso) return '—';
  return new Date(`${iso}T00:00:00`).toLocaleDateString([], {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export function PersonalInfoCard() {
  const currentUser = useCurrentUser();
  const user = currentUser.data;

  const fields = [
    { label: 'Name', value: user ? `${user.firstName} ${user.lastName}` : '—' },
    { label: 'Username', value: user ? `@${user.username}` : '—' },
    { label: 'Phone number', value: user?.phoneNumber ?? '—' },
    { label: 'Date of birth', value: formatDateOfBirth(user?.dateOfBirth) },
  ];

  return (
    <div className="border-border bg-card divide-y rounded-xl border">
      {fields.map(({ label, value }) => (
        <div key={label} className="flex items-center justify-between gap-4 p-4">
          <span className="text-muted-foreground text-sm">{label}</span>
          <span className="text-foreground text-base font-medium">{value}</span>
        </div>
      ))}
    </div>
  );
}
