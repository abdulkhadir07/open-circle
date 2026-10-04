import { LoaderCircle } from 'lucide-react';
import { Logo } from '@/components/ui/logo';

export function AuthLoadingScreen() {
  return (
    <main className="bg-background flex min-h-svh items-center justify-center px-5">
      <output className="flex items-center gap-3">
        <Logo size={28} />
        <span className="text-primary font-semibold">OpenCircle</span>
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-4 animate-spin" />
        <span className="sr-only">Checking your session</span>
      </output>
    </main>
  );
}
