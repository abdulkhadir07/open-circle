import { Pin, User } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { ProfileAvatarLink } from '@/features/profile/components/ProfileLink';
import { Card } from '@/components/ui/card';
import type { ChatRoom } from '../api/contracts';

export function otherParticipantNames(room: ChatRoom, currentUserId: string | undefined): string {
  // Keeps showing who you were talking to even after they leave the room.
  const others = room.participants.filter((p) => p.userId !== currentUserId);
  if (others.length === 0) return 'Just you';
  return others.map((p) => p.username).join(', ');
}

type ChatRoomRowProps = {
  room: ChatRoom;
  currentUserId: string | undefined;
  /** A single icon-only action (Hide, or Unhide) — revealed on hover/focus. */
  actions?: ReactNode;
};

export function ChatRoomRow({ room, currentUserId, actions }: ChatRoomRowProps) {
  const names = otherParticipantNames(room, currentUserId);
  const others = room.participants.filter((p) => p.userId !== currentUserId);
  // A profile link (and a real photo, as opposed to a group's generic
  // letter) naming one specific person only makes sense for a 1:1 chat.
  const soleOtherParticipant = others.length === 1 ? others[0] : undefined;
  const soleOtherParticipantId = soleOtherParticipant?.userId;

  return (
    <li className="group">
      <Card className="hover:border-primary/50 relative flex items-center gap-3 p-4 transition">
        <Link
          to={`/chats/${room.id}`}
          aria-label={`Open chat with ${names}`}
          className="absolute inset-0 rounded-2xl"
        />
        {soleOtherParticipant ? (
          <ProfileAvatarLink userId={soleOtherParticipant.userId} className="relative z-10">
            <Avatar name={names} profileImage={soleOtherParticipant.profileImage} />
          </ProfileAvatarLink>
        ) : (
          <Avatar name={names} />
        )}
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-1.5 text-sm font-semibold">
            {room.saved ? (
              <Pin aria-hidden="true" className="text-primary size-3.5 shrink-0" />
            ) : null}
            <span className="truncate">{names}</span>
          </p>
        </div>
        {room.closed ? (
          <span className="bg-muted text-muted-foreground rounded-full px-2 py-0.5 text-xs">
            Closed
          </span>
        ) : null}
        <div className="relative z-10 flex items-center gap-1 opacity-0 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100">
          {soleOtherParticipantId ? (
            <Link
              to={`/profile/${soleOtherParticipantId}`}
              aria-label={`View ${names}'s profile`}
              className="text-muted-foreground hover:bg-muted hover:text-foreground rounded-full p-1.5"
            >
              <User aria-hidden="true" className="size-4" />
            </Link>
          ) : null}
          {actions}
        </div>
      </Card>
    </li>
  );
}
