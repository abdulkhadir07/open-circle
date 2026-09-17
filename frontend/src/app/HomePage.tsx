import { LoaderCircle, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';

export function HomePage() {
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const logout = useLogout();

  async function handleLogout() {
    await logout.mutateAsync().catch(() => undefined);
    navigate('/login', { replace: true });
  }

  return (
    <section className="flex max-w-2xl flex-col items-start gap-6">
      <div className="space-y-2">
        <p className="text-primary text-sm font-semibold">Your circle</p>
        <h1 className="text-foreground text-3xl font-semibold">
          Welcome back{currentUser.data?.firstName ? `, ${currentUser.data.firstName}` : ''}
        </h1>
        {currentUser.data?.email ? (
          <p className="text-muted-foreground">Signed in as {currentUser.data.email}</p>
        ) : null}
      </div>
      <Button
        type="button"
        variant="outline"
        className="h-10 px-4"
        onClick={() => void handleLogout()}
        disabled={logout.isPending}
      >
        {logout.isPending ? (
          <LoaderCircle aria-hidden="true" className="animate-spin" />
        ) : (
          <LogOut aria-hidden="true" />
        )}
        {logout.isPending ? 'Signing out' : 'Sign out'}
      </Button>
    </section>
  );
}
