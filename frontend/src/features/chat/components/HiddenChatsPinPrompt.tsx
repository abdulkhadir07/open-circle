import { Lock } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useVerifyHiddenChatsPin } from '../hooks/useVerifyHiddenChatsPin';

export function HiddenChatsPinPrompt({ onUnlock }: { onUnlock: (pin: string) => void }) {
  const [pin, setPin] = useState('');
  const verifyPin = useVerifyHiddenChatsPin();

  function submit() {
    if (!pin || verifyPin.isPending) return;

    verifyPin.mutate(pin, { onSuccess: () => onUnlock(pin) });
  }

  return (
    <div className="flex flex-col items-center gap-3 px-2 py-6 text-center">
      <div className="bg-muted text-muted-foreground flex size-10 items-center justify-center rounded-full">
        <Lock aria-hidden="true" className="size-4.5" />
      </div>
      <div>
        <p className="text-foreground text-base font-semibold">Hidden chats are PIN-protected</p>
        <p className="text-muted-foreground mt-1 text-sm">Enter your PIN to view them.</p>
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        className="mt-1 w-full max-w-52 space-y-2"
      >
        <label htmlFor="hidden-chats-pin" className="sr-only">
          PIN
        </label>
        <Input
          id="hidden-chats-pin"
          type="password"
          inputMode="numeric"
          autoComplete="off"
          maxLength={6}
          placeholder="PIN"
          value={pin}
          onChange={(event) => setPin(event.target.value.replace(/\D/g, ''))}
          aria-invalid={verifyPin.isError || undefined}
          className="text-center tracking-[0.3em]"
        />
        <Button type="submit" className="w-full" disabled={!pin || verifyPin.isPending}>
          {verifyPin.isPending ? 'Checking…' : 'Unlock'}
        </Button>
      </form>

      {verifyPin.isError ? (
        <p role="alert" className="text-destructive text-sm">
          {verifyPin.error instanceof Error
            ? verifyPin.error.message
            : 'Unable to verify that PIN.'}
        </p>
      ) : null}
    </div>
  );
}
