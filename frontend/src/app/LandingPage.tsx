import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/logo';
import { SampleInvites } from './RotatingPrompts';

export function LandingPage() {
  return (
    <main className="flex min-h-svh items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="animate-fade-up text-center">
          <div className="flex justify-center">
            <Logo size={56} />
          </div>
          <h1 className="mt-4 text-4xl font-bold tracking-tight">OpenCircle</h1>
          <p className="text-muted-foreground mt-2">
            No followers. Just people on your campus to do things with today.
          </p>
          <p className="text-muted-foreground mt-1 text-sm">Sign up with your school email.</p>
        </div>

        <div
          className="animate-fade-up mt-8 flex flex-col gap-3 sm:flex-row"
          style={{ animationDelay: '80ms' }}
        >
          <Link
            to="/signup"
            className="bg-primary text-primary-foreground flex-1 rounded-lg px-4 py-3 text-center text-sm font-semibold transition hover:opacity-90 active:scale-95"
          >
            Create an account
          </Link>
          <Link
            to="/login"
            className="bg-card hover:bg-muted flex-1 rounded-lg border px-4 py-3 text-center text-sm font-semibold transition active:scale-95"
          >
            Log in
          </Link>
        </div>

        <SampleInvites />
      </div>
    </main>
  );
}
