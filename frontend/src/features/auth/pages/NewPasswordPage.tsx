import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '@/lib/api/errors';
import { AuthFormError } from '../components/AuthFormError';
import { AuthLayout } from '../components/AuthLayout';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { PasswordField } from '../components/PasswordField';
import { useResetPassword } from '../hooks/useResetPassword';
import { newPasswordSchema, type NewPasswordFormValues } from '../schemas/newPasswordSchema';

type NewPasswordRouteState = {
  email?: string;
  code?: string;
};

export function NewPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const routeState = location.state as NewPasswordRouteState | null;
  const resetPassword = useResetPassword();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<NewPasswordFormValues>({
    resolver: zodResolver(newPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  // Reaching this page requires the code entered on the previous step — a
  // direct or stale visit sends the user back there instead of failing here.
  if (!routeState?.email || !routeState.code) {
    return <Navigate to="/reset-password" replace state={{ email: routeState?.email }} />;
  }

  const { email, code } = routeState;

  const onSubmit = handleSubmit(async (values) => {
    try {
      await resetPassword.mutateAsync({ email, code, newPassword: values.newPassword });
      navigate('/login', { replace: true, state: { justReset: true } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setError('root', { message: `${error.message}. Go back and request a new code.` });
        return;
      }
      setError('root', {
        message: error instanceof Error ? error.message : 'Unable to reset your password.',
      });
    }
  });

  return (
    <AuthLayout title="Choose a new password" description={`Set a new password for ${email}.`}>
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <AuthFormError message={errors.root?.message} />

        <PasswordField
          id="new-password-password"
          label="New password"
          autoComplete="new-password"
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <PasswordField
          id="new-password-confirm"
          label="Confirm new password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <AuthSubmitButton
          type="submit"
          className="h-11 w-full px-4"
          pending={resetPassword.isPending}
          pendingLabel="Resetting"
        >
          Reset password
        </AuthSubmitButton>

        <p className="text-muted-foreground text-center text-sm">
          Wrong code?{' '}
          <Link
            to="/reset-password"
            state={{ email }}
            className="text-primary font-semibold hover:underline"
          >
            Go back
          </Link>
        </p>
      </form>
    </AuthLayout>
  );
}
