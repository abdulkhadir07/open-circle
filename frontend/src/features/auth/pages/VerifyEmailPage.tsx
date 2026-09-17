import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle, RotateCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/api/errors';
import { AnimatedError } from '../components/AnimatedError';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { VerificationCodeInput } from '../components/VerificationCodeInput';
import { useResendVerification } from '../hooks/useResendVerification';
import { useVerifyEmail } from '../hooks/useVerifyEmail';
import { verificationSchema, type VerificationFormValues } from '../schemas/verificationSchema';

type VerificationRouteState = {
  email?: string;
  from?: { pathname?: string; search?: string; hash?: string };
};

function destinationFromState(state: VerificationRouteState | null) {
  const path = state?.from?.pathname;
  if (!path?.startsWith('/') || path.startsWith('//')) return '/';
  return `${path}${state?.from?.search ?? ''}${state?.from?.hash ?? ''}`;
}

export function VerifyEmailPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const routeState = location.state as VerificationRouteState | null;
  const verify = useVerifyEmail();
  const resend = useResendVerification();
  const [cooldown, setCooldown] = useState(0);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const {
    register,
    control,
    trigger,
    getValues,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<VerificationFormValues>({
    resolver: zodResolver(verificationSchema),
    defaultValues: { email: routeState?.email ?? '', code: '' },
  });

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  const onSubmit = handleSubmit(async (values) => {
    clearErrors('root.server');
    clearErrors('code');
    try {
      await verify.mutateAsync(values);
      navigate(destinationFromState(routeState), { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setError('code', { message: error.message });
        return;
      }
      setError('root.server', {
        message: error instanceof Error ? error.message : 'Unable to verify your email.',
      });
    }
  });

  async function handleResend() {
    setResendMessage(null);
    const emailValid = await trigger('email');
    if (!emailValid) return;

    try {
      await resend.mutateAsync({ email: getValues('email') });
      clearErrors('code');
      setCooldown(30);
      setResendMessage('A new verification code has been sent.');
    } catch (error) {
      setResendMessage(error instanceof Error ? error.message : 'Unable to send a new code.');
    }
  }

  return (
    <AuthLayout
      title="Verify your email"
      description="Enter the 6-digit code we sent you. Codes expire after 15 minutes."
  
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <AuthFormError message={errors.root?.server?.message} />
        <AuthFormField
          id="verification-email"
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />

        <div className="space-y-2">
          <label htmlFor="verification-code" className="text-foreground block text-sm font-medium">
            Verification code
          </label>
          <Controller
            name="code"
            control={control}
            render={({ field }) => (
              <VerificationCodeInput
                id="verification-code"
                value={field.value}
                onChange={field.onChange}
                invalid={Boolean(errors.code)}
                describedBy={errors.code ? 'verification-code-error' : undefined}
              />
            )}
          />
          <AnimatedError id="verification-code-error" message={errors.code?.message} />
        </div>

        <AuthSubmitButton
          type="submit"
          className="h-11 w-full px-4"
          disabled={verify.isPending || resend.isPending}
          pending={verify.isPending}
          pendingLabel="Verifying"
        >
          Verify email
        </AuthSubmitButton>

        <div className="border-border flex items-center justify-between gap-4 border-t pt-5">
          <p className="text-muted-foreground text-sm" aria-live="polite">
            {resendMessage ?? "Didn't receive it?"}
          </p>
          <Button
            type="button"
            variant="ghost"
            className="h-9 shrink-0 px-3"
            disabled={resend.isPending || verify.isPending || cooldown > 0}
            onClick={() => void handleResend()}
          >
            {resend.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : (
              <RotateCw aria-hidden="true" />
            )}
            {cooldown > 0 ? `Send again in ${cooldown}s` : 'Send again'}
          </Button>
        </div>
      </form>
    </AuthLayout>
  );
}
