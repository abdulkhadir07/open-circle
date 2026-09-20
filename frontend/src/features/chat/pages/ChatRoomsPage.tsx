import { ArrowLeft, MessageCircle } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { Link, Outlet, useMatch } from 'react-router-dom';
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';
import { ChatRoomList } from '../components/ChatRoomList';
import { HiddenChatsPinGate } from '../components/HiddenChatsPinGate';

const TAB_OPTIONS = [
  { value: 'active', label: 'Chats' },
  { value: 'hidden', label: 'Hidden' },
] as const;

type Tab = (typeof TAB_OPTIONS)[number]['value'];

export function ChatRoomsPage() {
  const reduceMotion = useReducedMotion();
  const [tab, setTab] = useState<Tab>('active');
  const roomMatch = useMatch('/chats/:roomId');
  const selectedRoomId = roomMatch?.params.roomId;

  return (
    // Fixed to the viewport (rather than sized against AppLayout's <main> padding) so this
    // split view can never grow taller than the screen and cause page-level scroll — only
    // the room list and the message area scroll, each within their own bounded pane.
    // top-[61px] matches AppLayout's rendered header height exactly (measured, not guessed).
    <div className="fixed top-[61px] right-0 bottom-0 left-0">
      <div className="mx-auto flex h-full max-w-6xl gap-6 px-4 pb-6 sm:px-6">
        <aside
          className={cn(
            'flex min-h-0 w-full flex-col lg:w-80 lg:shrink-0',
            selectedRoomId && 'hidden lg:flex',
          )}
        >
          <Link
            to="/"
            className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-sm font-medium"
          >
            <ArrowLeft aria-hidden="true" className="size-4" />
            Back
          </Link>

          <h1 className="text-foreground mt-3 text-2xl font-semibold">Chats</h1>

          <div
            role="radiogroup"
            aria-label="Chats"
            className="border-border mt-4 flex items-baseline gap-5 border-b"
          >
            {TAB_OPTIONS.map((option) => {
              const selected = option.value === tab;
              return (
                <button
                  key={option.value}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => setTab(option.value)}
                  className={cn(
                    'relative pb-2.5 text-base font-semibold tracking-tight transition-colors',
                    selected
                      ? 'text-foreground'
                      : 'text-muted-foreground/60 hover:text-muted-foreground',
                  )}
                >
                  {option.label}
                  {selected ? (
                    <motion.span
                      layoutId="chats-tab-underline"
                      className="bg-primary absolute inset-x-0 bottom-0 h-[2.5px] rounded-full"
                      transition={
                        reduceMotion
                          ? { duration: 0 }
                          : { duration: 0.35, ease: [0.22, 1, 0.36, 1] }
                      }
                    />
                  ) : null}
                </button>
              );
            })}
          </div>

          <div className="mt-4 min-h-0 flex-1">
            <ScrollArea>
              {tab === 'active' ? (
                <ChatRoomList selectedRoomId={selectedRoomId} />
              ) : (
                <HiddenChatsPinGate selectedRoomId={selectedRoomId} />
              )}
            </ScrollArea>
          </div>
        </aside>

        <section
          className={cn(
            'min-h-0 min-w-0 flex-1 flex-col',
            selectedRoomId ? 'flex' : 'hidden lg:flex',
          )}
        >
          <Outlet />
        </section>
      </div>
    </div>
  );
}

export function ChatRoomsEmptyState() {
  return (
    <div className="border-border bg-card text-muted-foreground flex h-full flex-col items-center justify-center gap-2 rounded-xl border">
      <MessageCircle aria-hidden="true" className="size-6" />
      <p className="text-base">Select a conversation to view messages.</p>
    </div>
  );
}
