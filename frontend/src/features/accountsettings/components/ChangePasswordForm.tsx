import { zodResolver } from '@hookform/resolvers/zod';
import { CheckCircle2, LoaderCircle } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/lib/api/errors';
import { useChangePassword } from '../hooks/useChangePassword';
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from '../schemas/changePasswordSchema';
import { PasswordInput } from './PasswordInput';

export function ChangePasswordForm() {
  const reduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const changePassword = useChangePassword();
  const [justSucceeded, setJustSucceeded] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await changePassword.mutateAsync({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      reset();
      setJustSucceeded(true);
    } catch (error) {
      setError('root', {
        message: error instanceof ApiError ? error.message : 'Unable to change your password.',
      });
    }
  });

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
            <p className="text-foreground text-lg font-semibold">Password changed</p>
            <p className="text-muted-foreground mt-1 text-sm">
              Your password was updated and every other device has been signed out.
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
          noValidate
          onSubmit={(event) => void onSubmit(event)}
          className="border-border bg-card space-y-4 rounded-xl border p-4"
        >
          {errors.root?.message ? (
            <p role="alert" className="text-destructive text-base">
              {errors.root.message}
            </p>
          ) : null}

          <PasswordInput
            id="settings-current-password"
            label="Current password"
            autoComplete="current-password"
            error={errors.currentPassword?.message}
            {...register('currentPassword')}
          />
          <PasswordInput
            id="settings-new-password"
            label="New password"
            autoComplete="new-password"
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />
          <PasswordInput
            id="settings-confirm-password"
            label="Confirm new password"
            autoComplete="new-password"
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <Button type="submit" className="h-10 px-4" disabled={changePassword.isPending}>
            {changePassword.isPending ? (
              <LoaderCircle aria-hidden="true" className="animate-spin" />
            ) : null}
            {changePassword.isPending ? 'Saving' : 'Change password'}
          </Button>
        </motion.form>
      )}
    </AnimatePresence>
  );
}
