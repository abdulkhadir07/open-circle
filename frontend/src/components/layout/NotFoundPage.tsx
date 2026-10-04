import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/logo';

export function NotFoundPage() {
  return (
    <div className="animate-fade-up flex flex-col items-center gap-3 py-16 text-center">
      <Logo size={48} />
      <h1 className="text-2xl font-bold">Page not found</h1>
      <p className="text-muted-foreground text-sm">The page you're looking for doesn't exist.</p>
      <Link
        to="/"
        className="bg-primary text-primary-foreground mt-2 rounded-lg px-4 py-2 text-sm font-medium transition hover:opacity-90 active:scale-95"
      >
        Back to home
      </Link>
    </div>
  );
}
