import type { ReactNode } from 'react';
import { Circle } from 'lucide-react';
import { Link } from 'react-router-dom';

export function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="bg-background flex min-h-svh flex-col">
      <header className="border-border bg-background border-b">
        <div className="mx-auto flex w-full max-w-6xl items-center px-4 py-4 sm:px-6">
          <Link
            to="/"
            className="focus-visible:ring-ring/50 flex items-center rounded-sm outline-none focus-visible:ring-3"
          >
            <Circle aria-hidden="true" className="text-primary mr-2 size-5" strokeWidth={2.5} />
            <span className="text-primary text-lg font-semibold">OpenCircle</span>
          </Link>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-12">{children}</main>
    </div>
  );
}
