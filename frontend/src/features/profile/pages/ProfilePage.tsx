import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Calendar, Lock, LoaderCircle } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ApiError } from '@/lib/api/errors';
import { InterestChips } from '../components/InterestChips';
import { ProfileAwardsList } from '../components/ProfileAwardsList';
import { ProfileReputationSummary } from '../components/ProfileReputationSummary';
import { useProfile } from '../hooks/useProfile';
import { useUpdateMyProfile } from '../hooks/useUpdateMyProfile';
import {
  MAX_BIO_LENGTH,
  updateProfileSchema,
  type UpdateProfileFormValues,
} from '../schemas/updateProfileSchema';

function formatMemberSince(iso: string): string {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'long' });
}

export function ProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const reduceMotion = useReducedMotion();
  const currentUser = useCurrentUser();
  const profile = useProfile(userId);
  const updateProfile = useUpdateMyProfile();
  const [editing, setEditing] = useState(false);

  const isOwnProfile = Boolean(userId) && userId === currentUser.data?.id;

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<UpdateProfileFormValues>({
    resolver: zodResolver(updateProfileSchema),
    defaultValues: { displayName: '', bio: '', interests: [] },
  });

  function startEditing() {
    if (!profile.data) return;
    reset({
      displayName: profile.data.displayName,
      bio: profile.data.bio ?? '',
      interests: profile.data.interests,
    });
    setEditing(true);
  }

  const onSubmit = handleSubmit(async (values) => {
    try {
      await updateProfile.mutateAsync({
        displayName: values.displayName,
        bio: values.bio.trim() === '' ? null : values.bio.trim(),
        interests: values.interests,
      });
      setEditing(false);
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.interests) {
        setError('interests', { message: error.fieldErrors.interests });
      } else {
        setError('root', {
          message: error instanceof Error ? error.message : 'Unable to save your profile.',
        });
      }
    }
  });

  if (profile.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-6 animate-spin" />
      </div>
    );
  }

  if (profile.isError || !profile.data) {
    const notFound = profile.error instanceof ApiError && profile.error.status === 404;
    return (
      <div className="mx-auto max-w-2xl">
        <p role="alert" className="text-destructive text-base">
          {notFound
            ? "This profile doesn't exist."
            : profile.error instanceof Error
              ? profile.error.message
              : 'Unable to load this profile.'}
        </p>
      </div>
    );
  }

  const data = profile.data;

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back
      </Link>

      {/* Identity hero — the same gradient-tinted card treatment InvitePostCard
          uses for its primary subject, reused here for consistency. */}
      <div className="border-primary/15 from-card to-primary/[0.04] mt-4 rounded-2xl border bg-gradient-to-br p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <span className="bg-primary/10 text-primary flex size-20 shrink-0 items-center justify-center rounded-full text-3xl font-semibold">
              {data.displayName[0]?.toUpperCase()}
            </span>
            <div className="min-w-0">
              <h1 className="text-foreground truncate text-2xl font-semibold sm:text-3xl">
                {data.displayName}
              </h1>
              <p className="text-muted-foreground truncate text-base">@{data.username}</p>
              {isOwnProfile && currentUser.data ? (
                <p
                  title="Only visible to you"
                  className="text-muted-foreground mt-1.5 flex items-center gap-1.5 text-sm"
                >
                  <Lock aria-hidden="true" className="size-3.5" />
                  {currentUser.data.firstName} {currentUser.data.lastName}
                </p>
              ) : null}
              <p className="text-muted-foreground mt-1.5 flex items-center gap-1.5 text-sm">
                <Calendar aria-hidden="true" className="size-3.5" />
                Member since {formatMemberSince(data.memberSince)}
              </p>
            </div>
          </div>
          {isOwnProfile && !editing ? (
            <Button type="button" variant="outline" className="shrink-0" onClick={startEditing}>
              Edit profile
            </Button>
          ) : null}
        </div>

        <AnimatePresence mode="wait" initial={false}>
          {editing ? (
            <motion.form
              key="edit"
              initial={reduceMotion ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={reduceMotion ? undefined : { opacity: 0 }}
              transition={{ duration: reduceMotion ? 0 : 0.15 }}
              onSubmit={(event) => void onSubmit(event)}
              noValidate
              className="mt-6 space-y-4"
            >
              {errors.root?.message ? (
                <p role="alert" className="text-destructive text-base">
                  {errors.root.message}
                </p>
              ) : null}

              <div className="space-y-1.5">
                <label
                  htmlFor="profile-display-name"
                  className="text-foreground text-base font-medium"
                >
                  Display name
                </label>
                <Input
                  id="profile-display-name"
                  aria-invalid={Boolean(errors.displayName)}
                  {...register('displayName')}
                />
                {errors.displayName ? (
                  <p role="alert" className="text-destructive text-base">
                    {errors.displayName.message}
                  </p>
                ) : null}
              </div>

              <div className="space-y-1.5">
                <label htmlFor="profile-bio" className="text-foreground text-base font-medium">
                  Bio
                </label>
                <Controller
                  name="bio"
                  control={control}
                  render={({ field }) => (
                    <Textarea
                      id="profile-bio"
                      maxLength={MAX_BIO_LENGTH}
                      aria-invalid={Boolean(errors.bio)}
                      {...field}
                    />
                  )}
                />
                {errors.bio ? (
                  <p role="alert" className="text-destructive text-base">
                    {errors.bio.message}
                  </p>
                ) : null}
              </div>

              <div className="space-y-1.5">
                <label
                  htmlFor="profile-interests"
                  className="text-foreground text-base font-medium"
                >
                  Interests
                </label>
                <Controller
                  name="interests"
                  control={control}
                  render={({ field }) => (
                    <InterestChips
                      id="profile-interests"
                      value={field.value}
                      onChange={field.onChange}
                    />
                  )}
                />
                {errors.interests ? (
                  <p role="alert" className="text-destructive text-base">
                    {errors.interests.message}
                  </p>
                ) : null}
              </div>

              <div className="flex items-center gap-2">
                <Button type="submit" className="h-10 px-4" disabled={updateProfile.isPending}>
                  {updateProfile.isPending ? (
                    <LoaderCircle aria-hidden="true" className="animate-spin" />
                  ) : null}
                  {updateProfile.isPending ? 'Saving' : 'Save changes'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  className="h-10 px-4"
                  onClick={() => setEditing(false)}
                >
                  Cancel
                </Button>
              </div>
            </motion.form>
          ) : (
            <motion.div
              key="view"
              initial={reduceMotion ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={reduceMotion ? undefined : { opacity: 0 }}
              transition={{ duration: reduceMotion ? 0 : 0.15 }}
            >
              {data.bio ? (
                <p className="text-foreground mt-5 leading-6 whitespace-pre-wrap">{data.bio}</p>
              ) : null}

              {data.interests.length > 0 ? (
                <div className="mt-4 flex flex-wrap gap-1.5">
                  {data.interests.map((interest) => (
                    <span
                      key={interest}
                      className="bg-background text-foreground border-border rounded-full border px-2.5 py-1 text-sm"
                    >
                      {interest}
                    </span>
                  ))}
                </div>
              ) : null}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.3, delay: reduceMotion ? 0 : 0.05 }}
        className="mt-6"
      >
        <p className="text-muted-foreground text-xs font-semibold tracking-wide uppercase">
          Reputation
        </p>
        <div className="mt-2">
          <ProfileReputationSummary reputation={data.reputation} />
        </div>
      </motion.div>

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.3, delay: reduceMotion ? 0 : 0.1 }}
        className="mt-6"
      >
        <p className="text-muted-foreground text-xs font-semibold tracking-wide uppercase">
          Awards
        </p>
        <div className="mt-2">
          <ProfileAwardsList awards={data.awards} />
        </div>
      </motion.div>
    </div>
  );
}
