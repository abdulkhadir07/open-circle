import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle, Trash2, TriangleAlert } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/errors';
import { useDeleteAccount } from '../hooks/useDeleteAccount';
import { deleteAccountSchema, type DeleteAccountFormValues } from '../schemas/deleteAccountSchema';
import { PasswordInput } from './PasswordInput';

export function DeleteAccountForm() {
  const navigate = useNavigate();
  const deleteAccount = useDeleteAccount();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<DeleteAccountFormValues>({
    resolver: zodResolver(deleteAccountSchema),
    defaultValues: { currentPassword: '', confirmation: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await deleteAccount.mutateAsync(values);
      navigate('/login', { replace: true, state: { accountDeleted: true } });
    } catch (error) {
      setError('root', {
        message: error instanceof ApiError ? error.message : 'Unable to delete your account.',
      });
    }
  });

  return (
    <div className="space-y-4">
      <section className="border-destructive/40 bg-destructive/5 rounded-md border p-4">
        <div className="flex gap-3">
          <TriangleAlert aria-hidden="true" className="text-destructive mt-0.5 size-5 shrink-0" />
          <div className="space-y-3">
            <h2 className="text-foreground text-base font-semibold">This cannot be undone</h2>
            <p className="text-muted-foreground text-sm leading-6">
              Deleting your account is immediate. OpenCircle will permanently delete all account
              information stored about you in its active systems, including your profile, contact
              details, posts, messages, photos, ratings, account credentials, and active sessions.
              Your profile and account details will not be retained in the active service after
              deletion.
            </p>
            <p className="text-muted-foreground text-sm leading-6">
              Conversations connected to invites you created will also be permanently removed for
              their participants.
            </p>
          </div>
        </div>
      </section>

      <form
        noValidate
        onSubmit={(event) => void onSubmit(event)}
        className="bg-card space-y-4 rounded-md border p-4"
      >
        {errors.root?.message ? (
          <p role="alert" className="text-destructive text-sm">
            {errors.root.message}
          </p>
        ) : null}

        <PasswordInput
          id="delete-account-password"
          label="Current password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...register('currentPassword')}
        />

        <div className="space-y-1.5">
          <label
            htmlFor="delete-account-confirmation"
            className="text-muted-foreground block text-xs font-medium"
          >
            Type DELETE to confirm
          </label>
          <Input
            id="delete-account-confirmation"
            autoComplete="off"
            aria-invalid={Boolean(errors.confirmation)}
            {...register('confirmation')}
          />
          {errors.confirmation?.message ? (
            <p role="alert" className="text-destructive text-sm">
              {errors.confirmation.message}
            </p>
          ) : null}
        </div>

        <Button
          type="submit"
          variant="destructive"
          className="h-10 px-4"
          disabled={deleteAccount.isPending}
        >
          {deleteAccount.isPending ? (
            <LoaderCircle aria-hidden="true" className="animate-spin" />
          ) : (
            <Trash2 aria-hidden="true" />
          )}
          {deleteAccount.isPending ? 'Deleting account' : 'Permanently delete account'}
        </Button>
      </form>
    </div>
  );
}
