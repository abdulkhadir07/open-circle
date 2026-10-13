import { Check, Link2, Lock } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import type { UserProfile } from '../api/contracts';
import { EditableProfileAvatar } from './EditableProfileAvatar';

type ProfileHeaderProps = {
  profile: UserProfile;
  isOwnProfile: boolean;
  /** The viewer's real name, shown only to them and marked private. */
  privateName?: string;
  editing: boolean;
  onEdit: () => void;
};

const COPIED_RESET_MS = 2000;

export function ProfileHeader({
  profile,
  isOwnProfile,
  privateName,
  editing,
  onEdit,
}: ProfileHeaderProps) {
  const [copied, setCopied] = useState(false);
  const [photoOpen, setPhotoOpen] = useState(false);

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), COPIED_RESET_MS);
    return () => window.clearTimeout(timer);
  }, [copied]);

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/profile/${profile.userId}`);
      setCopied(true);
    } catch {
      // Clipboard access can be blocked; there's nothing useful to show beyond not flipping to "Copied".
    }
  }

  const avatarClassName = 'size-24 text-4xl ring-4';

  return (
    <div className="bg-card overflow-hidden rounded-2xl border">
      <div
        aria-hidden="true"
        className="from-primary to-gold h-28 bg-gradient-to-br via-[#a855f7] sm:h-36"
      />
      <div className="px-5 pb-5 sm:px-6">
        <div className="flex items-end justify-between gap-3">
          <div className="-mt-12 shrink-0">
            {isOwnProfile ? (
              <EditableProfileAvatar
                name={profile.displayName}
                profileImage={profile.profileImage}
                className={avatarClassName}
                onView={profile.profileImage ? () => setPhotoOpen(true) : undefined}
              />
            ) : profile.profileImage ? (
              <button
                type="button"
                aria-label={`View ${profile.displayName}'s profile photo`}
                onClick={() => setPhotoOpen(true)}
                className="focus-visible:ring-ring rounded-full outline-none focus-visible:ring-2 focus-visible:ring-offset-2"
              >
                <Avatar
                  name={profile.displayName}
                  profileImage={profile.profileImage}
                  className={avatarClassName}
                />
              </button>
            ) : (
              <Avatar
                name={profile.displayName}
                profileImage={profile.profileImage}
                className={avatarClassName}
              />
            )}
          </div>
          <div className="flex min-w-0 shrink-0 items-center gap-2 pt-3">
            <Button type="button" variant="outline" onClick={() => void copyLink()}>
              {copied ? <Check aria-hidden="true" /> : <Link2 aria-hidden="true" />}
              <span className="max-sm:sr-only">{copied ? 'Copied' : 'Copy link'}</span>
            </Button>
            {isOwnProfile && !editing ? (
              <Button type="button" onClick={onEdit}>
                Edit profile
              </Button>
            ) : null}
          </div>
        </div>

        <div className="mt-3 min-w-0">
          <h1 className="truncate text-2xl font-bold sm:text-3xl">{profile.displayName}</h1>
          <p className="text-muted-foreground truncate text-base">@{profile.username}</p>
          {isOwnProfile && privateName ? (
            <p
              title="Only visible to you"
              className="text-muted-foreground mt-1.5 flex items-center gap-1.5 text-sm"
            >
              <Lock aria-hidden="true" className="size-3.5" />
              {privateName}
            </p>
          ) : null}
        </div>
      </div>

      {profile.profileImage ? (
        <Dialog open={photoOpen} onOpenChange={setPhotoOpen}>
          <DialogContent className="max-w-2xl overflow-hidden bg-black p-0" showCloseButton>
            <DialogTitle className="sr-only">
              {profile.displayName}&apos;s profile photo
            </DialogTitle>
            <img
              src={profile.profileImage.url}
              alt={profile.displayName}
              className="max-h-[80svh] min-h-64 w-full object-contain"
            />
          </DialogContent>
        </Dialog>
      ) : null}
    </div>
  );
}
