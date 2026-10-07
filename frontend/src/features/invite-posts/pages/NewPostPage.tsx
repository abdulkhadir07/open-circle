import { SafetyBlockedNotice } from '@/features/ai/components/SafetyBlockedNotice';
import type { InviteDraft } from '@/features/ai/api/contracts';
import { useInviteDraft } from '@/features/ai/hooks/useInviteDraft';
import { AnimatedError } from '@/features/auth/components/AnimatedError';
import { isContentBlockedError } from '@/lib/api/errors';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { LoaderCircle, RotateCw, Sparkles } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
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
import { LocationVerificationPrompt } from '@/features/location/components/LocationVerificationPrompt';
import type { LocationScope } from '../api/contracts';
import { invitePostQueryKeys } from '../api/queryKeys';
import { PostImagePicker } from '../components/PostImagePicker';
import { TagPicker } from '../components/TagPicker';
import { useCreateInvitePost } from '../hooks/useCreateInvitePost';
import { useUploadInvitePostImage } from '../hooks/useUploadInvitePostImage';
import { postAudienceOptions } from '../lib/audience';
import { MAX_TAGS, parseTagList } from '../lib/tags';
import {
  createInvitePostSchema,
  type CreateInvitePostFormValues,
} from '../schemas/createInvitePostSchema';

const CONTENT_MAX_LENGTH = 500;

const INVITE_TYPE_OPTIONS = [
  { value: 'SINGLE', label: 'Single' },
  { value: 'GROUP', label: 'Group' },
] as const;

const EXAMPLES = [
  'Anyone up for coffee this afternoon?',
  'Looking for a walking buddy after work',
  'Pickup volleyball at the beach, need 4 more',
  'Trying a new ramen spot tonight, 2 people',
];

// Group invites have no backend-enforced upper bound, but a dropdown needs
// a finite list — 20 comfortably covers a real invite-post group size.
// "Custom" opens a plain number input for anything larger.
const CAPACITY_OPTIONS = Array.from({ length: 19 }, (_, index) => String(index + 2));
const CUSTOM_CAPACITY_VALUE = 'CUSTOM';

const labelClass = 'text-muted-foreground mb-1 block text-xs font-medium';
const errorClass = 'text-destructive text-sm';

type FailedUpload = { file: File; error: string };

/** Topics prefilled by the Home page idea chips (`?tags=coffee,food`). */
function initialTags(param: string | null): string[] {
  return param ? parseTagList(param.split(',')) : [];
}

/** The audience prefilled by Home's empty state (`?audience=GLOBAL`); anything else starts on City. */
function initialScope(param: string | null, stateRegionAvailable: boolean): LocationScope {
  if (param === 'COUNTRY' || param === 'GLOBAL') return param;
  if (param === 'STATE_REGION' && stateRegionAvailable) return param;
  return 'CITY';
}

export function NewPostPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const createInvitePost = useCreateInvitePost();
  const draftInvite = useInviteDraft();
  const uploadImage = useUploadInvitePostImage();
  const [isCustomCapacity, setIsCustomCapacity] = useState(false);
  const [stagedImages, setStagedImages] = useState<File[]>([]);
  const [uploadingImages, setUploadingImages] = useState(false);
  const [createdPostId, setCreatedPostId] = useState<string | null>(null);
  const [failedUploads, setFailedUploads] = useState<FailedUpload[]>([]);
  const [retryingIndex, setRetryingIndex] = useState<number | null>(null);

  const stateRegionAvailable = Boolean(currentUser.data?.verifiedStateRegion);
  const scopeOptions = useMemo(
    () => postAudienceOptions(currentUser.data ?? {}),
    [currentUser.data],
  );

  const schema = useMemo(
    () => createInvitePostSchema(stateRegionAvailable),
    [stateRegionAvailable],
  );

  const {
    register,
    control,
    handleSubmit,
    setValue,
    getValues,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<CreateInvitePostFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      content: (searchParams.get('text') ?? '').slice(0, CONTENT_MAX_LENGTH),
      inviteType: 'SINGLE',
      totalCapacity: '',
      locationScope: initialScope(searchParams.get('audience'), stateRegionAvailable),
      tags: initialTags(searchParams.get('tags')),
    },
  });

  const inviteType = useWatch({ control, name: 'inviteType' });
  const content = useWatch({ control, name: 'content' });

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
        tags: values.tags,
      });

      if (stagedImages.length === 0) {
        navigate('/');
        return;
      }

      setUploadingImages(true);
      const failures = await uploadStagedImages(post.id, stagedImages);
      setUploadingImages(false);
      void queryClient.invalidateQueries({ queryKey: invitePostQueryKeys.all });

      if (failures.length === 0) {
        navigate('/');
        return;
      }

      // The post already exists and there's no way to add photos to it later
      // (no edit flow, no delete-and-retry) — so a partial failure can't just
      // leave the page as if nothing happened.
      setCreatedPostId(post.id);
      setFailedUploads(failures);
    } catch (error) {
      setError('root', {
        // Safety Guardian refusals get a friendlier notice than a plain error line.
        type: isContentBlockedError(error) ? 'content-blocked' : 'server',
        message: error instanceof Error ? error.message : 'Unable to create your post.',
      });
    }
  });

  // Fills the form from the AI draft. A reply that arrives after the person kept typing is ignored.
  function applyDraft(requestedText: string, draft: InviteDraft) {
    if (getValues('content').trim() !== requestedText) return;
    if (createInvitePost.isPending || uploadingImages) return;

    const options = { shouldDirty: true, shouldValidate: true } as const;
    const capacity = draft.inviteType === 'GROUP' ? (draft.totalCapacity ?? 3) : null;

    setValue('content', draft.content.slice(0, CONTENT_MAX_LENGTH), options);
    setValue('inviteType', draft.inviteType, options);
    setValue('tags', parseTagList(draft.tags), options);
    setValue('totalCapacity', capacity === null ? '' : String(capacity), options);
    // The size dropdown only lists up to 20; larger sizes need the custom number box.
    setIsCustomCapacity(capacity !== null && capacity > CAPACITY_OPTIONS.length + 1);
    clearErrors('root');
  }

  function handleDraft() {
    const text = getValues('content').trim();
    if (!text || draftInvite.isPending) return;

    draftInvite.mutate(text, { onSuccess: (draft) => applyDraft(text, draft) });
  }

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
        navigate('/');
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

  if (currentUser.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <LoaderCircle aria-hidden="true" className="text-muted-foreground size-6 animate-spin" />
      </div>
    );
  }

  if (!currentUser.data?.locationVerifiedAt) {
    return <LocationVerificationPrompt />;
  }

  if (failedUploads.length > 0) {
    return (
      <div>
        <PageHeader
          title="Your invite was posted"
          sub={
            failedUploads.length === 1
              ? "One photo couldn't be uploaded. You can retry it or finish without it."
              : `${failedUploads.length} photos couldn't be uploaded. You can retry them or finish without them.`
          }
        />
        <Card className="animate-fade-up space-y-3">
          {failedUploads.map((failed, index) => (
            <div
              key={`${failed.file.name}-${index}`}
              className="flex items-center justify-between gap-3 rounded-xl border p-3"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{failed.file.name}</p>
                <p className={errorClass}>{failed.error}</p>
              </div>
              <Button
                type="button"
                variant="outline"
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
          <Button type="button" className="w-full" onClick={() => navigate('/')}>
            Done
          </Button>
        </Card>
      </div>
    );
  }

  const busy = createInvitePost.isPending || uploadingImages;

  return (
    <div>
      <PageHeader
        title="Start an invite"
        sub="Invite posts are visible for 24 hours, then they disappear on their own."
      />

      <form onSubmit={onSubmit} noValidate className="space-y-4">
        {errors.root?.message ? (
          errors.root.type === 'content-blocked' ? (
            <SafetyBlockedNotice message={errors.root.message} />
          ) : (
            <p role="alert" className={errorClass}>
              {errors.root.message}
            </p>
          )
        ) : null}

        <Card className="animate-fade-up">
          <label htmlFor="post-content" className={labelClass}>
            What's the invite?
          </label>
          <Textarea
            id="post-content"
            rows={4}
            maxLength={CONTENT_MAX_LENGTH}
            placeholder="What do you want to do, and when?"
            aria-invalid={Boolean(errors.content)}
            aria-describedby="post-content-count"
            {...register('content')}
          />
          <div className="text-muted-foreground mt-2 flex items-center justify-between text-xs">
            <span>Need ideas? Tap one:</span>
            <span
              id="post-content-count"
              className={content.length > 400 ? 'text-destructive' : undefined}
            >
              {content.length}/{CONTENT_MAX_LENGTH}
            </span>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDraft}
              disabled={content.trim() === '' || draftInvite.isPending}
            >
              {draftInvite.isPending ? (
                <LoaderCircle aria-hidden="true" className="animate-spin" />
              ) : (
                <Sparkles aria-hidden="true" />
              )}
              {draftInvite.isPending ? 'Drafting…' : 'Draft with AI'}
            </Button>
            <span className="text-muted-foreground text-xs">
              Turns your sentence into a clean invite with a size and topics.
            </span>
          </div>
          <AnimatedError
            message={
              draftInvite.isError
                ? draftInvite.error instanceof Error
                  ? draftInvite.error.message
                  : 'Unable to draft that right now.'
                : undefined
            }
          />
          <div className="mt-3 flex flex-wrap gap-1.5">
            {EXAMPLES.map((example) => (
              <button
                key={example}
                type="button"
                onClick={() => setValue('content', example, { shouldDirty: true })}
                className="text-muted-foreground hover:border-primary/50 hover:text-foreground bg-card cursor-pointer rounded-full border px-3 py-1 text-xs transition active:scale-95"
              >
                {example}
              </button>
            ))}
          </div>
          {errors.content ? (
            <p role="alert" className={`${errorClass} mt-2`}>
              {errors.content.message}
            </p>
          ) : null}
        </Card>

        <Card className="animate-fade-up" style={{ animationDelay: '30ms' }}>
          <label htmlFor="post-tags" className={labelClass}>
            Topics <span className="font-normal">(optional, up to {MAX_TAGS})</span>
          </label>
          <Controller
            name="tags"
            control={control}
            render={({ field }) => (
              <TagPicker
                id="post-tags"
                value={field.value}
                onChange={field.onChange}
                disabled={busy}
              />
            )}
          />
        </Card>

        <Card className="animate-fade-up space-y-4" style={{ animationDelay: '60ms' }}>
          <div>
            <span className={labelClass}>Invite type</span>
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
            <div>
              <label htmlFor="post-capacity" className={labelClass}>
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
                        className="shrink-0"
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
                <p role="alert" className={`${errorClass} mt-1`}>
                  {errors.totalCapacity.message}
                </p>
              ) : null}
            </div>
          ) : null}

          <div>
            <label htmlFor="post-location-scope" className={labelClass}>
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
              <p role="alert" className={`${errorClass} mt-1`}>
                {errors.locationScope.message}
              </p>
            ) : null}
          </div>

          <div>
            <span className={labelClass}>
              Photos <span className="font-normal">(optional)</span>
            </span>
            <PostImagePicker value={stagedImages} onChange={setStagedImages} disabled={busy} />
          </div>

          <div className="flex gap-2">
            <Button asChild variant="outline">
              <Link to="/">Cancel</Link>
            </Button>
            <Button type="submit" className="flex-1" disabled={busy}>
              {busy ? <LoaderCircle aria-hidden="true" className="animate-spin" /> : null}
              {createInvitePost.isPending
                ? 'Posting'
                : uploadingImages
                  ? 'Uploading photos'
                  : 'Post invite'}
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
