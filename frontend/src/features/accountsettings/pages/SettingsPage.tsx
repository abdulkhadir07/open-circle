import {
  ArrowLeft,
  ChevronRight,
  LoaderCircle,
  Lock,
  LogOut,
  Mail,
  Monitor,
  ShieldCheck,
  User,
} from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { useSessions } from '../hooks/useSessions';

export function SettingsPage() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const sessions = useSessions();
  const logout = useLogout();
  const hasPin = Boolean(currentUser.data?.hasHiddenChatsPin);
  const sessionCount = sessions.data?.length;

  async function handleSignOut() {
    await logout.mutateAsync().catch(() => undefined);
    navigate('/login', { replace: true });
  }

  const sections = [
    {
      to: '/settings/personal-info',
      icon: User,
      title: 'Personal info',
      description: currentUser.data
        ? `${currentUser.data.firstName} ${currentUser.data.lastName}`
        : 'Name, username, phone number, and date of birth',
    },
    {
      to: '/settings/password',
      icon: Lock,
      title: 'Password',
      description: 'Change your account password',
    },
    {
      to: '/settings/email',
      icon: Mail,
      title: 'Email',
      description: currentUser.data?.email ?? 'Update the email address on your account',
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
  ] as const;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back
      </Link>

      <p className="text-primary mt-4 text-sm font-semibold">Your account</p>
      <h1 className="text-foreground text-3xl font-semibold">Settings</h1>

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.3 }}
        className="border-border bg-card mt-6 divide-y rounded-xl border"
      >
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link
            key={to}
            to={to}
            className="hover:bg-muted/50 focus-visible:ring-ring flex items-center gap-4 p-4 transition-colors outline-none first:rounded-t-xl last:rounded-b-xl focus-visible:ring-2 focus-visible:-outline-offset-2"
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
      </motion.div>

      <div className="border-border mt-6 border-t pt-6">
        <Button
          type="button"
          variant="outline"
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
    </div>
  );
}
