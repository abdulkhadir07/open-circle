import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle, RotateCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { AnimatedError } from '../components/AnimatedError';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { VerificationCodeInput } from '../components/VerificationCodeInput';
import { useForgotPassword } from '../hooks/useForgotPassword';
import { resetCodeSchema, type ResetCodeFormValues } from '../schemas/resetCodeSchema';

type ResetPasswordRouteState = {
  email?: string;
};

export function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const routeState = location.state as ResetPasswordRouteState | null;
  const resend = useForgotPassword();
  const [cooldown, setCooldown] = useState(0);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const {
    register,
    control,
    trigger,
    getValues,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetCodeFormValues>({
    resolver: zodResolver(resetCodeSchema),
    defaultValues: { email: routeState?.email ?? '', code: '' },
  });

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  const onSubmit = handleSubmit((values) => {
    navigate('/reset-password/new-password', { state: { email: values.email, code: values.code } });
  });

  async function handleResend() {
    setResendMessage(null);
    const emailValid = await trigger('email');
    if (!emailValid) return;

    try {
      await resend.mutateAsync({ email: getValues('email') });
      setCooldown(30);
      setResendMessage('A new reset code has been sent.');
    } catch (error) {
      setResendMessage(error instanceof Error ? error.message : 'Unable to send a new code.');
    }
  }

  return (
    <AuthLayout
      title="Reset your password"
      description="If an account exists for that email, we've sent a 6-digit code. Codes expire after 15 minutes."
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <AuthFormError message={errors.root?.message} />
        <AuthFormField
          id="reset-password-email"
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />

        <div className="space-y-2">
          <label
            htmlFor="reset-password-code"
            className="text-foreground block text-sm font-medium"
          >
            Reset code
          </label>
          <Controller
            name="code"
            control={control}
            render={({ field }) => (
              <VerificationCodeInput
                id="reset-password-code"
                value={field.value}
                onChange={field.onChange}
                invalid={Boolean(errors.code)}
                describedBy={errors.code ? 'reset-password-code-error' : undefined}
              />
            )}
          />
          <AnimatedError id="reset-password-code-error" message={errors.code?.message} />
        </div>

        <AuthSubmitButton
          type="submit"
          className="h-11 w-full px-4"
          disabled={resend.isPending}
          pending={false}
          pendingLabel="Continuing"
        >
          Continue
        </AuthSubmitButton>

        <div className="border-border flex items-center justify-between gap-4 border-t pt-5">
          <p className="text-muted-foreground text-sm" aria-live="polite">
            {resendMessage ?? "Didn't receive it?"}
          </p>
          <Button
            type="button"
            variant="ghost"
            className="h-9 shrink-0 px-3"
            disabled={resend.isPending || cooldown > 0}
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
