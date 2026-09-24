import { CheckCircle2, LoaderCircle } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useSetHiddenChatsPin } from '@/features/chat/hooks/useSetHiddenChatsPin';
import { ApiError } from '@/lib/api/errors';
import { PasswordInput } from './PasswordInput';

const PIN_PATTERN = /^\d{4,6}$/;

export function HiddenChatsPinForm() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const setPin = useSetHiddenChatsPin();
  const hasPin = Boolean(currentUser.data?.hasHiddenChatsPin);

  const [currentPassword, setCurrentPassword] = useState('');
  const [pin, setPinValue] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const [justSucceeded, setJustSucceeded] = useState<'set' | 'changed' | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();

    if (!PIN_PATTERN.test(pin)) {
      setValidationError('PIN must be 4 to 6 digits.');
      return;
    }
    if (pin !== confirmPin) {
      setValidationError('PINs do not match.');
      return;
    }

    setValidationError(null);
    const wasChangingExistingPin = hasPin;
    setPin.mutate(
      { pin, currentPassword: hasPin ? currentPassword : undefined },
      {
        onSuccess: () => {
          setCurrentPassword('');
          setPinValue('');
          setConfirmPin('');
          setJustSucceeded(wasChangingExistingPin ? 'changed' : 'set');
        },
      },
    );
  }

  const errorMessage =
    validationError ??
    (setPin.isError
      ? setPin.error instanceof ApiError
        ? setPin.error.message
        : 'Unable to save that PIN.'
      : null);

  return (
    <AnimatePresence mode="wait" initial={false}>
      {justSucceeded ? (
        <motion.div
          key="success"
          initial={reduceMotion ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={reduceMotion ? undefined : { opacity: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.15 }}
          className="border-border bg-card flex flex-col items-center gap-3 rounded-xl border p-8 text-center"
        >
          <span className="flex size-12 items-center justify-center rounded-full bg-green-500/15 text-green-700 dark:text-green-400">
            <CheckCircle2 aria-hidden="true" className="size-6" />
          </span>
          <div>
            <p className="text-foreground text-lg font-semibold">
              {justSucceeded === 'set' ? 'PIN set' : 'PIN changed'}
            </p>
            <p className="text-muted-foreground mt-1 text-sm">
              {justSucceeded === 'set'
                ? 'Your hidden chats PIN is ready to use.'
                : 'Your hidden chats PIN was updated.'}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            className="mt-1 h-10 px-4"
            onClick={() => navigate('/settings')}
          >
            Close
          </Button>
        </motion.div>
      ) : (
        <motion.form
          key="form"
          initial={reduceMotion ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={reduceMotion ? undefined : { opacity: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.15 }}
          onSubmit={submit}
          className="border-border bg-card space-y-4 rounded-xl border p-4"
        >
          {errorMessage ? (
            <p role="alert" className="text-destructive text-base">
              {errorMessage}
            </p>
          ) : null}

          <p className="text-muted-foreground text-sm">
            A 4 to 6 digit PIN, separate from your account password, used to hide chats from view.
          </p>

          {hasPin ? (
            <PasswordInput
              id="settings-pin-current-password"
              label="Current password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          ) : null}

          <div className="space-y-1.5">
            <label htmlFor="settings-pin-new" className="text-foreground text-base font-medium">
              {hasPin ? 'New PIN' : 'PIN'}
            </label>
            <Input
              id="settings-pin-new"
              type="password"
              inputMode="numeric"
              autoComplete="off"
              maxLength={6}
              value={pin}
              onChange={(event) => setPinValue(event.target.value.replace(/\D/g, ''))}
              className="max-w-32 text-center tracking-[0.3em]"
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="settings-pin-confirm" className="text-foreground text-base font-medium">
              Confirm {hasPin ? 'new ' : ''}PIN
            </label>
            <Input
              id="settings-pin-confirm"
              type="password"
              inputMode="numeric"
              autoComplete="off"
              maxLength={6}
              value={confirmPin}
              onChange={(event) => setConfirmPin(event.target.value.replace(/\D/g, ''))}
              className="max-w-32 text-center tracking-[0.3em]"
            />
          </div>

          <Button
            type="submit"
            className="h-10 px-4"
            disabled={setPin.isPending || !pin || !confirmPin || (hasPin && !currentPassword)}
          >
            {setPin.isPending ? <LoaderCircle aria-hidden="true" className="animate-spin" /> : null}
            {setPin.isPending ? 'Saving' : hasPin ? 'Change PIN' : 'Set PIN'}
          </Button>
        </motion.form>
      )}
    </AnimatePresence>
  );
}
