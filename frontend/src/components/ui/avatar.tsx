import { cn } from 'cn';
import type { AriaAttributes } from 'react';

type AvatarProps = {
  name: string;
  profileImage?: { url: string } | null;
  className?: string;
  /** Set when adjacent text doesn't already name this person — e.g. a notification row whose message embeds the name elsewhere. */
  'aria-hidden'?: AriaAttributes['aria-hidden'];
};

function Avatar({ name, profileImage, className, 'aria-hidden': ariaHidden }: AvatarProps) {
  if (profileImage?.url) {
    return (
      <img
        data-slot="avatar"
        src={profileImage.url}
        alt=""
        aria-hidden={ariaHidden}
        className={cn('shrink-0 rounded-full object-cover', className)}
      />
    );
  }

  return (
    <span
      data-slot="avatar"
      aria-hidden={ariaHidden}
      className={cn(
        'flex shrink-0 items-center justify-center rounded-full font-semibold',
        className,
      )}
    >
      {name[0]?.toUpperCase()}
    </span>
  );
}

export { Avatar };
