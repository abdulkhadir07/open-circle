import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '@/lib/api/errors';
import { AuthFormError } from '../components/AuthFormError';
import { AuthFormField } from '../components/AuthFormField';
import { AuthLayout } from '../components/AuthLayout';
import { AuthSubmitButton } from '../components/AuthSubmitButton';
import { PasswordField } from '../components/PasswordField';
import { useLogin } from '../hooks/useLogin';
import { loginSchema, type LoginFormValues } from '../schemas/loginSchema';

type AuthRouteState = {
  from?: { pathname?: string; search?: string; hash?: string };
};

function intendedDestination(state: unknown) {
  const routeState = state as AuthRouteState | null;
  const path = routeState?.from?.pathname;
  if (!path?.startsWith('/') || path.startsWith('//')) return '/';
  return `${path}${routeState?.from?.search ?? ''}${routeState?.from?.hash ?? ''}`;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useLogin();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await login.mutateAsync(values);
      navigate(intendedDestination(location.state), { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) {
        navigate('/verify-email', {
          replace: true,
          state: { email: values.email, from: (location.state as AuthRouteState | null)?.from },
        });
        return;
      }

      setError('root.server', {
        message: error instanceof Error ? error.message : 'Unable to sign in. Please try again.',
      });
    }
  });

  return (
    <AuthLayout
      title="Welcome back"
      description="Sign in to see your invites, conversations, and circles."
      footer={
        <p>
          New to OpenCircle?{' '}
          <Link to="/signup" className="text-primary font-semibold hover:underline">
            Create an account
          </Link>
        </p>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-5">
        <AuthFormError message={errors.root?.server?.message} />
        <AuthFormField
          id="login-email"
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <PasswordField
          id="login-password"
          label="Password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <AuthSubmitButton
          type="submit"
          className="h-11 w-full px-4"
          pending={login.isPending}
          pendingLabel="Signing in"
        >
          Sign in
        </AuthSubmitButton>
      </form>
    </AuthLayout>
  );
}
