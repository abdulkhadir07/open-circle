import {
  Bell,
  Home,
  LogOut,
  MessageCircle,
  Plus,
  Settings,
  Star,
  Trophy,
  User,
  Users,
  type LucideIcon,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Logo } from '@/components/ui/logo';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { useUnreadNotificationCount } from '@/features/notifications/hooks/useUnreadNotificationCount';
import { useMyScore } from '@/features/scoreboard/hooks/useMyScore';
import { EditableProfileAvatar } from '@/features/profile/components/EditableProfileAvatar';
import { cn } from '@/lib/utils';

type NavItem = { to: string; label: string; icon: LucideIcon; active: (path: string) => boolean };

function NavLinks({
  items,
  pathname,
  unread,
  compact = false,
}: {
  items: NavItem[];
  pathname: string;
  unread: number;
  compact?: boolean;
}) {
  return (
    <>
      {items.map(({ to, label, icon: Icon, active }) => {
        const badge =
          label === 'Notifications' && unread > 0 ? (unread > 9 ? '9+' : String(unread)) : null;
        return (
          <Link
            key={to}
            to={to}
            title={label}
            aria-label={badge ? `${label}, ${unread} unread` : label}
            aria-current={active(pathname) ? 'page' : undefined}
            className={cn(
              'relative flex items-center gap-3 rounded-lg text-sm font-medium transition',
              compact ? 'shrink-0 p-2' : 'px-3 py-2',
              active(pathname)
                ? 'bg-primary/12 text-primary'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
            )}
          >
            <Icon aria-hidden="true" className="size-4.5" />
            {compact ? null : label}
            {badge ? (
              <span
                aria-hidden="true"
                className={cn(
                  'bg-primary text-primary-foreground flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 text-xs font-semibold tabular-nums',
                  compact ? 'absolute -top-0.5 -right-0.5 h-4 min-w-4 px-1 text-[10px]' : 'ml-auto',
                )}
              >
                {badge}
              </span>
            ) : null}
          </Link>
        );
      })}
    </>
  );
}

export function AppLayout({ children, fill = false }: { children: ReactNode; fill?: boolean }) {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const logout = useLogout();
  const currentUser = useCurrentUser();
  const unread = useUnreadNotificationCount().data ?? 0;
  const myScore = useMyScore().data;
  const user = currentUser.data;
  const name = user ? `${user.firstName} ${user.lastName}` : '';

  const items: NavItem[] = [
    { to: '/', label: 'Home', icon: Home, active: (path) => path === '/' },
    {
      to: '/requests',
      label: 'Requests',
      icon: Users,
      active: (path) => path.startsWith('/requests'),
    },
    {
      to: '/chats',
      label: 'Chats',
      icon: MessageCircle,
      active: (path) => path.startsWith('/chats'),
    },
    {
      to: '/notifications',
      label: 'Notifications',
      icon: Bell,
      active: (path) => path.startsWith('/notifications'),
    },
    { to: '/ratings', label: 'Ratings', icon: Star, active: (path) => path.startsWith('/ratings') },
    {
      to: '/scoreboard',
      label: 'Scoreboard',
      icon: Trophy,
      active: (path) => path.startsWith('/scoreboard'),
    },
    ...(user
      ? [
          {
            to: `/profile/${user.id}`,
            label: 'Profile',
            icon: User,
            active: (path: string) => path.startsWith('/profile/'),
          },
        ]
      : []),
    {
      to: '/settings',
      label: 'Settings',
      icon: Settings,
      active: (path) => path.startsWith('/settings'),
    },
  ];

  async function handleLogout() {
    await logout.mutateAsync().catch(() => undefined);
    navigate('/login', { replace: true });
  }

  const hideFloatingCreate = pathname === '/new' || pathname.startsWith('/chats/');

  return (
    <div className="bg-background min-h-svh md:flex">
      <aside className="bg-card sticky top-0 hidden h-svh w-60 shrink-0 flex-col border-r p-4 md:flex">
        <Link to="/" className="mb-6 flex items-center gap-2 px-2 text-lg font-bold">
          <Logo size={32} />
          OpenCircle
        </Link>

        {user ? (
          <div className="bg-muted/60 mb-5 flex items-center gap-3 rounded-xl p-3">
            <EditableProfileAvatar
              name={name}
              profileImage={user.profileImage}
              className="size-9 text-sm"
            />
            <Link to={`/profile/${user.id}`} className="min-w-0 flex-1 hover:underline">
              <div className="truncate text-sm font-semibold">{name}</div>
              <div className="text-muted-foreground truncate text-xs">
                {myScore ? <span>{myScore.annualScore} pts · </span> : null}
                <span>@{user.username}</span>
              </div>
            </Link>
          </div>
        ) : null}

        <Link
          to="/new"
          className="bg-primary text-primary-foreground shadow-primary/30 mb-4 flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold shadow-md transition hover:-translate-y-0.5 hover:shadow-lg active:scale-95"
        >
          <Plus aria-hidden="true" className="size-5" />
          Start an invite
        </Link>

        <nav aria-label="Main" className="flex flex-col gap-1">
          <NavLinks items={items} pathname={pathname} unread={unread} />
        </nav>

        <button
          type="button"
          onClick={() => void handleLogout()}
          disabled={logout.isPending}
          className="text-muted-foreground hover:bg-muted hover:text-foreground mt-auto flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm transition disabled:opacity-50"
        >
          <LogOut aria-hidden="true" className="size-4.5" />
          Log out
        </button>
      </aside>

      <header className="bg-card sticky top-0 z-10 flex h-14 items-center gap-1 overflow-x-auto border-b px-3 md:hidden">
        <Link to="/" aria-label="OpenCircle home" className="mr-1 shrink-0">
          <Logo size={28} />
        </Link>
        <nav aria-label="Main" className="flex items-center gap-1">
          <NavLinks items={items} pathname={pathname} unread={unread} compact />
        </nav>
        <button
          type="button"
          title="Log out"
          aria-label="Log out"
          onClick={() => void handleLogout()}
          disabled={logout.isPending}
          className="text-muted-foreground hover:text-foreground ml-auto shrink-0 cursor-pointer p-2 disabled:opacity-50"
        >
          <LogOut aria-hidden="true" className="size-4.5" />
        </button>
      </header>

      <main
        className={cn(
          'min-w-0 flex-1',
          'mx-auto w-full max-w-3xl p-4 md:p-8',
          !fill && 'pb-24 md:pb-28',
        )}
      >
        {children}
      </main>

      {hideFloatingCreate ? null : (
        <Link
          to="/new"
          aria-label="Start an invite"
          className="group bg-primary text-primary-foreground shadow-primary/40 fixed right-5 bottom-5 z-20 flex items-center gap-2 rounded-full p-4 shadow-xl transition hover:scale-105 active:scale-95 md:right-8 md:bottom-8 md:px-5"
        >
          <Plus aria-hidden="true" className="size-6 transition group-hover:rotate-90" />
          <span className="hidden text-sm font-semibold md:inline">Start an invite</span>
        </Link>
      )}
    </div>
  );
}
