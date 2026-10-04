import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Logo } from '@/components/ui/logo';

type AuthLayoutProps = {
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
};

export function AuthLayout({ title, description, children, footer }: AuthLayoutProps) {
  return (
    <main className="flex min-h-svh items-center justify-center p-4">
      <div data-slot="auth-content" className="animate-fade-up w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <Link to="/" aria-label="OpenCircle home">
            <Logo size={48} />
          </Link>
          <h1 className="text-2xl font-bold">{title}</h1>
          <p className="text-muted-foreground text-sm">{description}</p>
        </div>
        <Card>{children}</Card>
        {footer ? (
          <div className="text-muted-foreground mt-4 text-center text-sm">{footer}</div>
        ) : null}
      </div>
    </main>
  );
}
