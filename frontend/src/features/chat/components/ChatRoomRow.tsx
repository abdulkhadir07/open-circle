import { Pin, User } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
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
  isSelected?: boolean;
  /** A single icon-only action (Hide, or Unhide) — revealed on hover/focus. */
  actions?: ReactNode;
};

export function ChatRoomRow({
  room,
  currentUserId,
  isSelected = false,
  actions,
}: ChatRoomRowProps) {
  const names = otherParticipantNames(room, currentUserId);
  const others = room.participants.filter((p) => p.userId !== currentUserId);
  // A profile link naming one specific person only makes sense for a 1:1 chat.
  const soleOtherParticipantId = others.length === 1 ? others[0]?.userId : undefined;

  return (
    <li
      className={cn(
        'group flex items-center gap-1 rounded-lg pr-2 transition-colors',
        isSelected ? 'bg-primary/10' : 'hover:bg-muted',
      )}
    >
      <Link to={`/chats/${room.id}`} className="flex min-w-0 flex-1 items-center gap-3 px-2 py-2.5">
        <span className="bg-accent/20 text-accent flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
          {names[0]?.toUpperCase()}
        </span>
        <div className="min-w-0 flex-1">
          <p className="text-foreground flex items-center gap-1.5 text-base font-semibold">
            {room.saved ? (
              <Pin aria-hidden="true" className="text-primary size-3.5 shrink-0" />
            ) : null}
            <span className="truncate">{names}</span>
          </p>
          {room.closed ? <p className="text-muted-foreground mt-0.5 text-sm">Closed</p> : null}
        </div>
      </Link>
      {soleOtherParticipantId ? (
        <Link
          to={`/profile/${soleOtherParticipantId}`}
          aria-label={`View ${names}'s profile`}
          className="text-muted-foreground hover:bg-muted hover:text-foreground shrink-0 rounded-full p-1.5 opacity-0 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100"
        >
          <User aria-hidden="true" className="size-4" />
        </Link>
      ) : null}
      {actions ? (
        <span className="shrink-0 opacity-0 transition-opacity group-focus-within:opacity-100 group-hover:opacity-100">
          {actions}
        </span>
      ) : null}
    </li>
  );
}
