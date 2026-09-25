import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { useForgotPassword } from '../hooks/useForgotPassword';
import {
  forgotPasswordSchema,
  type ForgotPasswordFormValues,
} from '../schemas/forgotPasswordSchema';

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const forgotPassword = useForgotPassword();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      // The backend always returns 204 here regardless of whether the email
      // belongs to an account — that's deliberate (no account-enumeration
      // leak), so a resolved request always moves on to the code-entry step.
      await forgotPassword.mutateAsync(values);
      navigate('/reset-password', { state: { email: values.email } });
    } catch (error) {
      setError('root.server', {
        message: error instanceof Error ? error.message : 'Unable to send a reset code.',
      });
    }
  });

  return (
    <AuthLayout
      title="Reset your password"
      description="Enter your email and we'll send you a code to reset your password."
      footer={
        <p>
          Remembered it?{' '}
          <Link to="/login" className="text-primary font-semibold hover:underline">
            Sign in
          </Link>
        </p>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <AuthFormError message={errors.root?.server?.message} />
        <AuthFormField
          id="forgot-password-email"
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <AuthSubmitButton
          type="submit"
          className="h-11 w-full px-4"
          pending={forgotPassword.isPending}
          pendingLabel="Sending"
        >
          Send reset code
        </AuthSubmitButton>
      </form>
    </AuthLayout>
  );
}
