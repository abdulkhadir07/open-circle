import { zodResolver } from '@hookform/resolvers/zod';
import { LoaderCircle } from 'lucide-react';
import { useEffect, useMemo } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { SafetyBlockedNotice } from '@/features/ai/components/SafetyBlockedNotice';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
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
import { isContentBlockedError } from '@/lib/api/errors';
import type { InvitePost } from '../api/contracts';
import { useUpdateInvitePost } from '../hooks/useUpdateInvitePost';
import { postAudienceOptions } from '../lib/audience';
import { MAX_TAGS } from '../lib/tags';
import {
  createInvitePostSchema,
  type CreateInvitePostFormValues,
} from '../schemas/createInvitePostSchema';
import { TagPicker } from './TagPicker';

const INVITE_TYPE_OPTIONS = [
  { value: 'SINGLE', label: 'Single' },
  { value: 'GROUP', label: 'Group' },
] as const;

const labelClass = 'text-muted-foreground mb-1 block text-xs font-medium';
const errorClass = 'text-destructive mt-1 text-sm';

type EditInvitePostDialogProps = {
  post: InvitePost;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function EditInvitePostDialog({ post, open, onOpenChange }: EditInvitePostDialogProps) {
  const currentUser = useCurrentUser();
  const updatePost = useUpdateInvitePost();
  const verifiedPlace = useMemo(
    () =>
      currentUser.data ?? {
        verifiedCity: post.city,
        verifiedStateRegion: post.stateRegion ?? undefined,
        verifiedCountry: post.country,
      },
    [currentUser.data, post.city, post.country, post.stateRegion],
  );
  const stateRegionAvailable = Boolean(verifiedPlace.verifiedStateRegion);
  const schema = useMemo(
    () => createInvitePostSchema(stateRegionAvailable),
    [stateRegionAvailable],
  );
  const audienceOptions = useMemo(() => postAudienceOptions(verifiedPlace), [verifiedPlace]);

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
    defaultValues: {
      content: post.content,
      inviteType: post.inviteType,
      totalCapacity: post.inviteType === 'GROUP' ? String(post.totalCapacity) : '',
      locationScope: post.locationScope,
      tags: post.tags,
    },
  });

  const inviteType = useWatch({ control, name: 'inviteType' });

  useEffect(() => {
    if (!open) return;
    reset({
      content: post.content,
      inviteType: post.inviteType,
      totalCapacity: post.inviteType === 'GROUP' ? String(post.totalCapacity) : '',
      locationScope: post.locationScope,
      tags: post.tags,
    });
  }, [open, post, reset]);

  const onSubmit = handleSubmit(async (values) => {
    clearErrors('root');
    const totalCapacity = values.inviteType === 'GROUP' ? Number(values.totalCapacity) : 1;
    if (totalCapacity < post.acceptedCount) {
      setError('totalCapacity', {
        message: `Capacity cannot be lower than ${post.acceptedCount} already accepted`,
      });
      return;
    }

    try {
      await updatePost.mutateAsync({
        postId: post.id,
        request: {
          content: values.content,
          inviteType: values.inviteType,
          totalCapacity,
          locationScope: values.locationScope,
          tags: values.tags,
        },
      });
      onOpenChange(false);
    } catch (error) {
      setError('root', {
        type: isContentBlockedError(error) ? 'content-blocked' : 'server',
        message: error instanceof Error ? error.message : 'Unable to update this invite.',
      });
    }
  });

  const busy = updatePost.isPending;

  return (
    <Dialog open={open} onOpenChange={(next) => !busy && onOpenChange(next)}>
      <DialogContent
        aria-describedby={undefined}
        className="max-h-[90svh] overflow-y-auto sm:max-w-xl"
      >
        <DialogHeader>
          <DialogTitle>Edit invite</DialogTitle>
        </DialogHeader>

        <form onSubmit={(event) => void onSubmit(event)} noValidate className="space-y-4">
          {errors.root?.message ? (
            errors.root.type === 'content-blocked' ? (
              <SafetyBlockedNotice message={errors.root.message} />
            ) : (
              <p role="alert" className={errorClass}>
                {errors.root.message}
              </p>
            )
          ) : null}

          <div>
            <label htmlFor="edit-post-content" className={labelClass}>
              What's the invite?
            </label>
            <Textarea
              id="edit-post-content"
              rows={4}
              maxLength={500}
              disabled={busy}
              aria-invalid={Boolean(errors.content)}
              {...register('content')}
            />
            {errors.content ? (
              <p role="alert" className={errorClass}>
                {errors.content.message}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="edit-post-tags" className={labelClass}>
              Topics <span className="font-normal">(optional, up to {MAX_TAGS})</span>
            </label>
            <Controller
              name="tags"
              control={control}
              render={({ field }) => (
                <TagPicker
                  id="edit-post-tags"
                  value={field.value}
                  onChange={field.onChange}
                  disabled={busy}
                />
              )}
            />
          </div>

          <div>
            <span className={labelClass}>Invite type</span>
            <Controller
              name="inviteType"
              control={control}
              render={({ field }) => (
                <SegmentedControl
                  aria-label="Edit invite type"
                  value={field.value}
                  onChange={field.onChange}
                  options={INVITE_TYPE_OPTIONS}
                />
              )}
            />
          </div>

          {inviteType === 'GROUP' ? (
            <div>
              <label htmlFor="edit-post-capacity" className={labelClass}>
                How many people can join?
              </label>
              <Input
                id="edit-post-capacity"
                type="number"
                min={Math.max(2, post.acceptedCount)}
                inputMode="numeric"
                disabled={busy}
                aria-invalid={Boolean(errors.totalCapacity)}
                {...register('totalCapacity')}
              />
              {errors.totalCapacity ? (
                <p role="alert" className={errorClass}>
                  {errors.totalCapacity.message}
                </p>
              ) : null}
            </div>
          ) : null}

          <div>
            <label htmlFor="edit-post-location-scope" className={labelClass}>
              Who can see it
            </label>
            <Controller
              name="locationScope"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange} disabled={busy}>
                  <SelectTrigger
                    id="edit-post-location-scope"
                    aria-invalid={Boolean(errors.locationScope)}
                  >
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {audienceOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.locationScope ? (
              <p role="alert" className={errorClass}>
                {errors.locationScope.message}
              </p>
            ) : null}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={busy}
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? <LoaderCircle aria-hidden="true" className="animate-spin" /> : null}
              {busy ? 'Saving' : 'Save changes'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
