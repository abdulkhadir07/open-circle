import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle, RotateCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { VerificationCodeInput } from '@/features/auth/components/VerificationCodeInput';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ApiError } from '@/lib/api/errors';
import { useRequestEmailChange } from '../hooks/useRequestEmailChange';
import { useVerifyEmailChange } from '../hooks/useVerifyEmailChange';
import {
  requestEmailChangeSchema,
  type RequestEmailChangeFormValues,
} from '../schemas/requestEmailChangeSchema';
import { PasswordInput } from './PasswordInput';

export function ChangeEmailForm() {
  const currentUser = useCurrentUser();
  const requestEmailChange = useRequestEmailChange();
  const verifyEmailChange = useVerifyEmailChange();

  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState<string | undefined>();
  const [cooldown, setCooldown] = useState(0);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    getValues,
    formState: { errors },
  } = useForm<RequestEmailChangeFormValues>({
    resolver: zodResolver(requestEmailChangeSchema),
    defaultValues: { newEmail: '', currentPassword: '' },
  });

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  const onSubmitRequest = handleSubmit(async (values) => {
    try {
      await requestEmailChange.mutateAsync(values);
      setPendingEmail(values.newEmail);
      setCode('');
      setCodeError(undefined);
      setCooldown(30);
    } catch (error) {
      setError('root', {
        message: error instanceof ApiError ? error.message : 'Unable to start the email change.',
      });
    }
  });

  async function handleResend() {
    const values = getValues();
    try {
      await requestEmailChange.mutateAsync(values);
      setCooldown(30);
      setCodeError(undefined);
    } catch (error) {
      setCodeError(error instanceof ApiError ? error.message : 'Unable to send a new code.');
    }
  }

  async function handleVerify() {
    setCodeError(undefined);
    try {
      await verifyEmailChange.mutateAsync(code);
      setPendingEmail(null);
      setCode('');
      reset();
    } catch (error) {
      setCodeError(error instanceof ApiError ? error.message : 'Unable to verify that code.');
    }
  }

  if (pendingEmail) {
    return (
      <div className="border-border bg-card space-y-4 rounded-xl border p-4">
        <p className="text-foreground text-base">
          Enter the 6-digit code we sent to <span className="font-medium">{pendingEmail}</span>.
          Codes expire after 15 minutes.
        </p>

        <VerificationCodeInput
          id="settings-email-change-code"
          value={code}
          onChange={setCode}
          invalid={Boolean(codeError)}
          describedBy={codeError ? 'settings-email-change-code-error' : undefined}
        />
        {codeError ? (
          <p
            id="settings-email-change-code-error"
            role="alert"
            className="text-destructive text-sm"
          >
            {codeError}
          </p>
        ) : null}

        <div className="flex items-center gap-2">
          <Button
            type="button"
            className="h-10 px-4"
            disabled={verifyEmailChange.isPending || code.length !== 6}
            onClick={() => void handleVerify()}
          >
            {verifyEmailChange.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : null}
            {verifyEmailChange.isPending ? 'Verifying' : 'Verify email'}
          </Button>
          <Button
            type="button"
            variant="outline"
            className="h-10 px-4"
            onClick={() => {
              setPendingEmail(null);
              setCode('');
              setCodeError(undefined);
            }}
          >
            Cancel
          </Button>
        </div>

        <div className="border-border flex items-center justify-between gap-4 border-t pt-4">
          <p className="text-muted-foreground text-sm">Didn't receive it?</p>
          <Button
            type="button"
            variant="ghost"
            className="h-9 shrink-0 px-3"
            disabled={requestEmailChange.isPending || cooldown > 0}
            onClick={() => void handleResend()}
          >
            {requestEmailChange.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : (
              <RotateCw aria-hidden="true" />
            )}
            {cooldown > 0 ? `Send again in ${cooldown}s` : 'Send again'}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <form
      noValidate
      onSubmit={(event) => void onSubmitRequest(event)}
      className="border-border bg-card space-y-4 rounded-xl border p-4"
    >
      {errors.root?.message ? (
        <p role="alert" className="text-destructive text-base">
          {errors.root.message}
        </p>
      ) : null}

      <p className="text-muted-foreground text-sm">
        Current email:{' '}
        <span className="text-foreground font-medium">{currentUser.data?.email}</span>
      </p>

      <div className="space-y-1.5">
        <label htmlFor="settings-new-email" className="text-foreground text-base font-medium">
          New email
        </label>
        <Input
          id="settings-new-email"
          type="email"
          autoComplete="email"
          aria-invalid={Boolean(errors.newEmail)}
          {...register('newEmail')}
        />
        {errors.newEmail ? (
          <p role="alert" className="text-destructive text-sm">
            {errors.newEmail.message}
          </p>
        ) : null}
      </div>

      <PasswordInput
        id="settings-email-current-password"
        label="Current password"
        autoComplete="current-password"
        error={errors.currentPassword?.message}
        {...register('currentPassword')}
      />

      <Button type="submit" className="h-10 px-4" disabled={requestEmailChange.isPending}>
        {requestEmailChange.isPending ? (
          <LoaderCircle aria-hidden="true" className="animate-spin" />
        ) : null}
        {requestEmailChange.isPending ? 'Sending' : 'Send verification code'}
      </Button>
    </form>
  );
}
