import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

type ProfileLinkProps = {
  userId: string;
  children: ReactNode;
  className?: string;
};

/** A person's name, linking to their profile. */
export function ProfileLink({ userId, children, className }: ProfileLinkProps) {
  return (
    <Link to={`/profile/${userId}`} className={cn('hover:underline', className)}>
      {children}
    </Link>
  );
}

type ProfileAvatarLinkProps = ProfileLinkProps & {
  /**
   * Accessible name, for avatars with no name link beside them (e.g. chat bubbles). When omitted
   * the link is hidden from keyboard and screen-reader navigation, since the name next to the
   * avatar already links to the same page.
   */
  label?: string;
};

/** Wraps an avatar so it links to the person's profile. */
export function ProfileAvatarLink({ userId, children, className, label }: ProfileAvatarLinkProps) {
  return (
    <Link
      to={`/profile/${userId}`}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      tabIndex={label ? undefined : -1}
      className={cn('shrink-0 rounded-full', className)}
    >
      {children}
    </Link>
  );
}
