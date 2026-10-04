import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useMyScore } from '@/features/scoreboard/hooks/useMyScore';
import { ApiError } from '@/lib/api/errors';
import { InterestChips } from '../components/InterestChips';
import { ProfileAwardsList } from '../components/ProfileAwardsList';
import { ProfileCompletion } from '../components/ProfileCompletion';
import { ProfileHeader } from '../components/ProfileHeader';
import { ProfileOpenInvites } from '../components/ProfileOpenInvites';
import { ProfileStats } from '../components/ProfileStats';
import { useProfile } from '../hooks/useProfile';
import { useUpdateMyProfile } from '../hooks/useUpdateMyProfile';
import {
  MAX_BIO_LENGTH,
  updateProfileSchema,
  type UpdateProfileFormValues,
} from '../schemas/updateProfileSchema';

export function ProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const reduceMotion = useReducedMotion();
  const currentUser = useCurrentUser();
  const profile = useProfile(userId);
  const myScore = useMyScore();
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
      <div>
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
  const privateName =
    isOwnProfile && currentUser.data
      ? `${currentUser.data.firstName} ${currentUser.data.lastName}`
      : undefined;
  const hasBio = Boolean(data.bio);
  const hasInterests = data.interests.length > 0;

  const about = (
    <section>
      <h2 className="text-muted-foreground mb-2 px-1 text-xs font-semibold tracking-wide uppercase">
        About
      </h2>
      <Card>
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
              className="space-y-4"
            >
              {errors.root?.message ? (
                <p role="alert" className="text-destructive text-base">
                  {errors.root.message}
                </p>
              ) : null}

              <div className="space-y-1.5">
                <label
                  htmlFor="profile-display-name"
                  className="text-muted-foreground mb-1 block text-xs font-medium"
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
                <label
                  htmlFor="profile-bio"
                  className="text-muted-foreground mb-1 block text-xs font-medium"
                >
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
                  className="text-muted-foreground mb-1 block text-xs font-medium"
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
              className="space-y-4"
            >
              {hasBio ? (
                <p className="text-foreground leading-6 whitespace-pre-wrap">{data.bio}</p>
              ) : (
                <p className="text-muted-foreground text-sm">
                  {isOwnProfile ? 'You haven’t added a bio yet.' : 'No bio yet.'}
                </p>
              )}

              {hasInterests ? (
                <div className="flex flex-wrap gap-1.5">
                  {data.interests.map((interest) => (
                    <span
                      key={interest}
                      className="bg-primary/10 text-primary rounded-full px-3 py-1 text-sm font-medium"
                    >
                      {interest}
                    </span>
                  ))}
                </div>
              ) : null}
            </motion.div>
          )}
        </AnimatePresence>
      </Card>
    </section>
  );

  return (
    <div className="space-y-6">
      <ProfileHeader
        profile={data}
        isOwnProfile={isOwnProfile}
        privateName={privateName}
        editing={editing}
        onEdit={startEditing}
      />

      {editing ? about : null}

      {isOwnProfile && !editing ? (
        <ProfileCompletion
          hasBio={hasBio}
          hasInterests={hasInterests}
          hasPhoto={Boolean(data.profileImage)}
          onEditProfile={startEditing}
        />
      ) : null}

      <ProfileStats
        reputation={data.reputation}
        awardsCount={data.awards.length}
        memberSince={data.memberSince}
        points={isOwnProfile ? myScore.data?.annualScore : undefined}
        rank={isOwnProfile ? myScore.data?.rank : undefined}
      />

      {!editing ? about : null}

      <section>
        <h2 className="text-muted-foreground mb-2 px-1 text-xs font-semibold tracking-wide uppercase">
          Awards
        </h2>
        <ProfileAwardsList awards={data.awards} />
      </section>

      <ProfileOpenInvites
        userId={data.userId}
        isOwnProfile={isOwnProfile}
        displayName={data.displayName}
      />
    </div>
  );
}
