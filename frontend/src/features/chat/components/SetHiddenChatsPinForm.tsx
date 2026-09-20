import { ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useSetHiddenChatsPin } from '../hooks/useSetHiddenChatsPin';

const PIN_PATTERN = /^\d{4,6}$/;

export function SetHiddenChatsPinForm({
  onSuccess,
  onCancel,
}: {
  onSuccess: (pin: string) => void;
  onCancel: () => void;
}) {
  const [pin, setPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const setPinMutation = useSetHiddenChatsPin();

  function submit() {
    if (setPinMutation.isPending) return;

    if (!PIN_PATTERN.test(pin)) {
      setValidationError('PIN must be 4 to 6 digits.');
      return;
    }
    if (pin !== confirmPin) {
      setValidationError('PINs do not match.');
      return;
    }

    setValidationError(null);
    setPinMutation.mutate({ pin }, { onSuccess: () => onSuccess(pin) });
  }

  const errorMessage =
    validationError ??
    (setPinMutation.isError
      ? setPinMutation.error instanceof Error
        ? setPinMutation.error.message
        : 'Unable to set that PIN.'
      : null);

  return (
    <div className="flex flex-col items-center gap-3 px-2 py-6 text-center">
      <div className="bg-muted text-muted-foreground flex size-10 items-center justify-center rounded-full">
        <ShieldCheck aria-hidden="true" className="size-4.5" />
      </div>
      <div>
        <p className="text-foreground text-base font-semibold">Set up a Hidden chats PIN</p>
        <p className="text-muted-foreground mt-1 text-sm">
          A 4 to 6 digit PIN, separate from your account password.
        </p>
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        className="mt-1 w-full max-w-64 space-y-2 text-left"
      >
        <div className="space-y-1">
          <label htmlFor="hidden-chats-pin-new" className="text-foreground text-sm font-medium">
            New PIN
          </label>
          <Input
            id="hidden-chats-pin-new"
            type="password"
            inputMode="numeric"
            autoComplete="off"
            maxLength={6}
            value={pin}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, ''))}
            className="text-center tracking-[0.3em]"
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="hidden-chats-pin-confirm" className="text-foreground text-sm font-medium">
            Confirm PIN
          </label>
          <Input
            id="hidden-chats-pin-confirm"
            type="password"
            inputMode="numeric"
            autoComplete="off"
            maxLength={6}
            value={confirmPin}
            onChange={(event) => setConfirmPin(event.target.value.replace(/\D/g, ''))}
            className="text-center tracking-[0.3em]"
          />
        </div>

        {errorMessage ? (
          <p role="alert" className="text-destructive text-sm">
            {errorMessage}
          </p>
        ) : null}

        <div className="flex gap-2 pt-1">
          <Button type="button" variant="outline" className="flex-1" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            type="submit"
            className="flex-1"
            disabled={setPinMutation.isPending || !pin || !confirmPin}
          >
            {setPinMutation.isPending ? 'Saving…' : 'Save PIN'}
          </Button>
        </div>
      </form>
    </div>
  );
}
