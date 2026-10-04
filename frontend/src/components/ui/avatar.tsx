import { cn } from 'cn';
import type { AriaAttributes } from 'react';

type AvatarProps = {
  name: string;
  profileImage?: { url: string } | null;
  className?: string;
  /** Set when adjacent text doesn't already name this person — e.g. a notification row whose message embeds the name elsewhere. */
  'aria-hidden'?: AriaAttributes['aria-hidden'];
};

const GRADIENTS = [
  ['#7c3aed', '#c084fc'],
  ['#f59e0b', '#f97316'],
  ['#0ea5e9', '#6366f1'],
  ['#10b981', '#06b6d4'],
  ['#ec4899', '#f43f5e'],
  ['#8b5cf6', '#ec4899'],
] as const;

function gradientFor(name: string) {
  const [from, to] =
    GRADIENTS[[...name].reduce((sum, ch) => sum + ch.charCodeAt(0), 0) % GRADIENTS.length]!;
  return `linear-gradient(135deg, ${from}, ${to})`;
}

function Avatar({ name, profileImage, className, 'aria-hidden': ariaHidden }: AvatarProps) {
  if (profileImage?.url) {
    return (
      <img
        data-slot="avatar"
        src={profileImage.url}
        alt=""
        aria-hidden={ariaHidden}
        className={cn('ring-card size-9 shrink-0 rounded-full object-cover ring-2', className)}
      />
    );
  }

  // The inline gradient/colour deliberately win over any tint classes call sites
  // still pass, so every letter avatar in the app looks the same.
  return (
    <span
      data-slot="avatar"
      aria-hidden={ariaHidden}
      style={{ background: gradientFor(name), color: '#fff' }}
      className={cn(
        'ring-card flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold ring-2',
        className,
      )}
    >
      {name.trim()[0]?.toUpperCase() ?? '?'}
    </span>
  );
}

export { Avatar };
