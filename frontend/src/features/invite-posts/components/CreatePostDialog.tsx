import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle, Plus, RotateCw } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useQueryClient } from '@tanstack/react-query';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { SegmentedControl } from '@/components/ui/segmented-control';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useCreateInvitePost } from '../hooks/useCreateInvitePost';
import { useUploadInvitePostImage } from '../hooks/useUploadInvitePostImage';
import {
  createInvitePostSchema,
  type CreateInvitePostFormValues,
} from '../schemas/createInvitePostSchema';
import { SCOPE_LABELS } from '../api/contracts';
import { invitePostQueryKeys } from '../api/queryKeys';
import { PostImagePicker } from './PostImagePicker';

const CONTENT_MAX_LENGTH = 500;

const INVITE_TYPE_OPTIONS = [
  { value: 'SINGLE', label: 'Single' },
  { value: 'GROUP', label: 'Group' },
] as const;

// Group invites have no backend-enforced upper bound, but a dropdown needs
// a finite list — 20 comfortably covers a real invite-post group size.
// "Custom" opens a plain number input for anything larger.
const CAPACITY_OPTIONS = Array.from({ length: 19 }, (_, index) => String(index + 2));
const CUSTOM_CAPACITY_VALUE = 'CUSTOM';

type FailedUpload = { file: File; error: string };

export function CreatePostDialog({ triggerClassName }: { triggerClassName?: string }) {
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const createInvitePost = useCreateInvitePost();
  const uploadImage = useUploadInvitePostImage();
  const [open, setOpen] = useState(false);
  const [isCustomCapacity, setIsCustomCapacity] = useState(false);
  const [stagedImages, setStagedImages] = useState<File[]>([]);
  const [uploadingImages, setUploadingImages] = useState(false);
  const [createdPostId, setCreatedPostId] = useState<string | null>(null);
  const [failedUploads, setFailedUploads] = useState<FailedUpload[]>([]);
  const [retryingIndex, setRetryingIndex] = useState<number | null>(null);

  const stateRegionAvailable = Boolean(currentUser.data?.verifiedStateRegion);
  const scopeOptions = useMemo(
    () =>
      (['CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL'] as const)
        .filter((scope) => scope !== 'STATE_REGION' || stateRegionAvailable)
        .map((scope) => ({ value: scope, label: SCOPE_LABELS[scope] })),
    [stateRegionAvailable],
  );

  const schema = useMemo(
    () => createInvitePostSchema(stateRegionAvailable),
    [stateRegionAvailable],
  );

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<CreateInvitePostFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { content: '', inviteType: 'SINGLE', totalCapacity: '', locationScope: 'CITY' },
  });

  const inviteType = useWatch({ control, name: 'inviteType' });
  const content = useWatch({ control, name: 'content' });

  function resetEverything() {
    reset();
    setIsCustomCapacity(false);
    setStagedImages([]);
    setUploadingImages(false);
    setCreatedPostId(null);
    setFailedUploads([]);
    setRetryingIndex(null);
  }

  async function uploadStagedImages(postId: string, files: File[]) {
    const failures: FailedUpload[] = [];

    for (const file of files) {
      try {
        await uploadImage.mutateAsync({ postId, file });
      } catch (error) {
        failures.push({
          file,
          error: error instanceof Error ? error.message : 'Unable to upload that photo.',
        });
      }
    }

    return failures;
  }

  const onSubmit = handleSubmit(async (values) => {
    clearErrors('root');
    try {
      const post = await createInvitePost.mutateAsync({
        content: values.content,
        inviteType: values.inviteType,
        totalCapacity: values.inviteType === 'GROUP' ? Number(values.totalCapacity) : 1,
        locationScope: values.locationScope,
      });

      if (stagedImages.length === 0) {
        resetEverything();
        setOpen(false);
        return;
      }

      setUploadingImages(true);
      const failures = await uploadStagedImages(post.id, stagedImages);
      setUploadingImages(false);
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });

      if (failures.length === 0) {
        resetEverything();
        setOpen(false);
        return;
      }

      // The post already exists and there's no way to add photos to it later
      // (no edit flow, no delete-and-retry) — so a partial failure can't just
      // close the dialog as if nothing happened.
      setCreatedPostId(post.id);
      setFailedUploads(failures);
    } catch (error) {
      setError('root', {
        message: error instanceof Error ? error.message : 'Unable to create your post.',
      });
    }
  });

  async function retryUpload(index: number) {
    if (!createdPostId) return;
    const failed = failedUploads[index];
    if (!failed) return;

    setRetryingIndex(index);
    try {
      await uploadImage.mutateAsync({ postId: createdPostId, file: failed.file });
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });

      const remaining = failedUploads.filter((_, i) => i !== index);
      if (remaining.length === 0) {
        resetEverything();
        setOpen(false);
        return;
      }
      setFailedUploads(remaining);
    } catch (error) {
      setFailedUploads((current) =>
        current.map((item, i) =>
          i === index
            ? {
                ...item,
                error: error instanceof Error ? error.message : 'Unable to upload that photo.',
              }
            : item,
        ),
      );
    } finally {
      setRetryingIndex(null);
    }
  }

  const hasFailedUploads = failedUploads.length > 0;

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) {
          resetEverything();
        }
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" className={cn('h-10 px-4', triggerClassName)}>
          <Plus aria-hidden="true" />
          New post
        </Button>
      </DialogTrigger>
      <DialogContent>
        {hasFailedUploads ? (
          <>
            <DialogHeader>
              <DialogTitle>Your invite was posted</DialogTitle>
              <DialogDescription>
                {failedUploads.length === 1
                  ? "One photo couldn't be uploaded. You can retry it or finish without it."
                  : `${failedUploads.length} photos couldn't be uploaded. You can retry them or finish without them.`}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-2">
              {failedUploads.map((failed, index) => (
                <div
                  key={`${failed.file.name}-${index}`}
                  className="border-border flex items-center justify-between gap-3 rounded-lg border p-3"
                >
                  <div className="min-w-0">
                    <p className="text-foreground truncate text-sm font-medium">
                      {failed.file.name}
                    </p>
                    <p className="text-destructive text-sm">{failed.error}</p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="h-9 shrink-0 px-3"
                    disabled={retryingIndex !== null}
                    onClick={() => void retryUpload(index)}
                  >
                    {retryingIndex === index ? (
                      <LoaderCircle aria-hidden="true" className="animate-spin" />
                    ) : (
                      <RotateCw aria-hidden="true" />
                    )}
                    Retry
                  </Button>
                </div>
              ))}
            </div>

            <DialogFooter>
              <Button
                type="button"
                className="h-10 px-4"
                onClick={() => {
                  resetEverything();
                  setOpen(false);
                }}
              >
                Done
              </Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>New invite post</DialogTitle>
              <DialogDescription>
                Invite posts are visible for 24 hours, then they disappear on their own.
              </DialogDescription>
            </DialogHeader>

            <form onSubmit={onSubmit} noValidate className="space-y-4">
              {errors.root?.message ? (
                <p role="alert" className="text-destructive text-base">
                  {errors.root.message}
                </p>
              ) : null}

              <div className="space-y-1.5">
                <label htmlFor="post-content" className="text-foreground text-base font-medium">
                  What's the invite?
                </label>
                <Textarea
                  id="post-content"
                  maxLength={CONTENT_MAX_LENGTH}
                  aria-invalid={Boolean(errors.content)}
                  aria-describedby="post-content-count"
                  {...register('content')}
                />
                <div className="flex items-center justify-between">
                  {errors.content ? (
                    <p role="alert" className="text-destructive text-base">
                      {errors.content.message}
                    </p>
                  ) : (
                    <span />
                  )}
                  <span id="post-content-count" className="text-muted-foreground text-sm">
                    {content.length}/{CONTENT_MAX_LENGTH}
                  </span>
                </div>
              </div>

              <div className="space-y-1.5">
                <span className="text-foreground block text-base font-medium">Invite type</span>
                <Controller
                  name="inviteType"
                  control={control}
                  render={({ field }) => (
                    <SegmentedControl
                      aria-label="Invite type"
                      value={field.value}
                      onChange={field.onChange}
                      options={INVITE_TYPE_OPTIONS}
                    />
                  )}
                />
              </div>

              {inviteType === 'GROUP' ? (
                <div className="space-y-1.5">
                  <label htmlFor="post-capacity" className="text-foreground text-base font-medium">
                    How many people can join?
                  </label>
                  <Controller
                    name="totalCapacity"
                    control={control}
                    render={({ field }) =>
                      isCustomCapacity ? (
                        <div className="flex gap-2">
                          <Input
                            id="post-capacity"
                            type="number"
                            min={2}
                            inputMode="numeric"
                            placeholder="e.g. 25"
                            className="flex-1"
                            aria-invalid={Boolean(errors.totalCapacity)}
                            value={field.value}
                            onChange={(event) => field.onChange(event.target.value)}
                          />
                          <Button
                            type="button"
                            variant="outline"
                            className="h-11 shrink-0 px-3 text-sm"
                            onClick={() => {
                              setIsCustomCapacity(false);
                              field.onChange('');
                            }}
                          >
                            Choose from list
                          </Button>
                        </div>
                      ) : (
                        <Select
                          value={field.value}
                          onValueChange={(value) => {
                            if (value === CUSTOM_CAPACITY_VALUE) {
                              setIsCustomCapacity(true);
                              field.onChange('');
                            } else {
                              field.onChange(value);
                            }
                          }}
                        >
                          <SelectTrigger
                            id="post-capacity"
                            aria-invalid={Boolean(errors.totalCapacity)}
                          >
                            <SelectValue placeholder="Select a group size" />
                          </SelectTrigger>
                          <SelectContent>
                            {CAPACITY_OPTIONS.map((capacity) => (
                              <SelectItem key={capacity} value={capacity}>
                                {capacity} people
                              </SelectItem>
                            ))}
                            <SelectItem value={CUSTOM_CAPACITY_VALUE}>Custom number…</SelectItem>
                          </SelectContent>
                        </Select>
                      )
                    }
                  />
                  {errors.totalCapacity ? (
                    <p role="alert" className="text-destructive text-base">
                      {errors.totalCapacity.message}
                    </p>
                  ) : null}
                </div>
              ) : null}

              <div className="space-y-1.5">
                <label
                  htmlFor="post-location-scope"
                  className="text-foreground text-base font-medium"
                >
                  Who can see it
                </label>
                <Controller
                  name="locationScope"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger
                        id="post-location-scope"
                        aria-invalid={Boolean(errors.locationScope)}
                      >
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {scopeOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.locationScope ? (
                  <p role="alert" className="text-destructive text-base">
                    {errors.locationScope.message}
                  </p>
                ) : null}
              </div>

              <div className="space-y-1.5">
                <span className="text-foreground block text-base font-medium">
                  Photos <span className="text-muted-foreground font-normal">(optional)</span>
                </span>
                <PostImagePicker
                  value={stagedImages}
                  onChange={setStagedImages}
                  disabled={createInvitePost.isPending || uploadingImages}
                />
              </div>

              <DialogFooter>
                <DialogClose asChild>
                  <Button type="button" variant="outline" className="h-10 px-4">
                    Cancel
                  </Button>
                </DialogClose>
                <Button
                  type="submit"
                  className="h-10 px-4"
                  disabled={createInvitePost.isPending || uploadingImages}
                >
                  {createInvitePost.isPending || uploadingImages ? (
                    <LoaderCircle aria-hidden="true" className="animate-spin" />
                  ) : null}
                  {createInvitePost.isPending
                    ? 'Posting'
                    : uploadingImages
                      ? 'Uploading photos'
                      : 'Post invite'}
                </Button>
              </DialogFooter>
            </form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
