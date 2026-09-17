import { Circle, LoaderCircle } from 'lucide-react';

export function AuthLoadingScreen() {
  return (
    <main className="bg-background flex min-h-svh items-center justify-center px-5">
      <output className="flex items-center gap-3">
        <Circle aria-hidden="true" className="text-primary size-5" strokeWidth={2.5} />
        <span className="text-primary font-semibold">OpenCircle</span>
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-4 animate-spin" />
        <span className="sr-only">Checking your session</span>
      </output>
    </main>
  );
}
