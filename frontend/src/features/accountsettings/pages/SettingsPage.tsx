import {
  ChevronRight,
  LoaderCircle,
  Lock,
  LogOut,
  Mail,
  Monitor,
  Palette,
  ShieldCheck,
  User,
  type LucideIcon,
} from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { Link, useNavigate } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { PageHeader } from '@/components/ui/page-header';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { getStoredTheme } from '@/lib/theme';
import { useSessions } from '../hooks/useSessions';

type SettingsRow = { to: string; icon: LucideIcon; title: string; description: string };

const THEME_LABELS = { system: 'System', light: 'Light', dark: 'Dark' } as const;

function SettingsGroup({ label, rows }: { label: string; rows: SettingsRow[] }) {
  return (
    <section className="mt-6">
      <h2 className="text-muted-foreground mb-2 px-1 text-xs font-semibold tracking-wide uppercase">
        {label}
      </h2>
      <Card className="divide-y overflow-hidden p-0">
        {rows.map(({ to, icon: Icon, title, description }) => (
          <Link
            key={to}
            to={to}
            className="hover:bg-muted/50 focus-visible:ring-ring flex items-center gap-4 p-4 transition-colors outline-none focus-visible:ring-2 focus-visible:-outline-offset-2"
          >
            <span className="bg-primary/10 text-primary flex size-10 shrink-0 items-center justify-center rounded-full">
              <Icon aria-hidden="true" className="size-4.5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="text-foreground block text-base font-medium">{title}</span>
              <span className="text-muted-foreground block truncate text-sm">{description}</span>
            </span>
            <ChevronRight aria-hidden="true" className="text-muted-foreground size-5 shrink-0" />
          </Link>
        ))}
      </Card>
    </section>
  );
}

export function SettingsPage() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const sessions = useSessions();
  const logout = useLogout();
  const user = currentUser.data;
  const hasPin = Boolean(user?.hasHiddenChatsPin);
  const sessionCount = sessions.data?.length;

  async function handleSignOut() {
    await logout.mutateAsync().catch(() => undefined);
    navigate('/login', { replace: true });
  }

  const account: SettingsRow[] = [
    {
      to: '/settings/personal-info',
      icon: User,
      title: 'Personal info',
      description: 'Name, username, phone number, and date of birth',
    },
    {
      to: '/settings/email',
      icon: Mail,
      title: 'Email',
      description: user?.email ?? 'Update the email address on your account',
    },
  ];

  const security: SettingsRow[] = [
    {
      to: '/settings/password',
      icon: Lock,
      title: 'Password',
      description: 'Change your account password',
    },
    {
      to: '/settings/pin',
      icon: ShieldCheck,
      title: 'Hidden chats PIN',
      description: hasPin ? 'PIN set — change it here' : 'No PIN set yet',
    },
    {
      to: '/settings/sessions',
      icon: Monitor,
      title: 'Sessions',
      description:
        sessionCount === undefined
          ? 'Manage the devices signed in to your account'
          : `${sessionCount} ${sessionCount === 1 ? 'device' : 'devices'} signed in`,
    },
  ];

  const appearance: SettingsRow[] = [
    {
      to: '/settings/appearance',
      icon: Palette,
      title: 'Theme',
      description: THEME_LABELS[getStoredTheme()],
    },
  ];

  return (
    <div>
      <PageHeader title="Settings" sub="Manage your account and how you sign in." />

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.3 }}
      >
        {user ? (
          <Card className="flex items-center gap-4">
            <Avatar
              name={`${user.firstName} ${user.lastName}`}
              profileImage={user.profileImage}
              className="size-14 text-xl"
            />
            <div className="min-w-0 flex-1">
              <p className="truncate text-lg font-semibold">
                {user.firstName} {user.lastName}
              </p>
              <p className="text-muted-foreground truncate text-sm">@{user.username}</p>
            </div>
            <Button asChild variant="outline" className="shrink-0">
              <Link to={`/profile/${user.id}`}>View profile</Link>
            </Button>
          </Card>
        ) : null}

        <SettingsGroup label="Account" rows={account} />
        <SettingsGroup label="Security" rows={security} />
        <SettingsGroup label="Appearance" rows={appearance} />

        <div className="mt-8">
          <Button
            type="button"
            variant="destructive"
            className="w-full sm:w-auto"
            onClick={() => void handleSignOut()}
            disabled={logout.isPending}
          >
            {logout.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : (
              <LogOut aria-hidden="true" />
            )}
            {logout.isPending ? 'Signing out' : 'Sign out'}
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
