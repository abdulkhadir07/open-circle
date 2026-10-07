import { LoaderCircle, Send } from 'lucide-react';
import { useState, type KeyboardEvent } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ContentError } from '@/features/ai/components/ContentError';
import { cn } from '@/lib/utils';
import { useCreateBanter } from '../hooks/useCreateBanter';

export const BANTER_MAX_LENGTH = 280;
const COUNTER_WARNING_AT = 250;

const PROMPTS = [
  'Best coffee spot nearby?',
  'Hot take:',
  'Overheard at the coffee shop...',
  'Who else is up too late?',
];

export function BanterComposer() {
  const currentUser = useCurrentUser();
  const createBanter = useCreateBanter();
  const [text, setText] = useState('');

  const trimmed = text.trim();
  const remaining = BANTER_MAX_LENGTH - text.length;

  function submit() {
    if (!trimmed || createBanter.isPending) return;
    createBanter.mutate(trimmed, { onSuccess: () => setText('') });
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
      event.preventDefault();
      submit();
    }
  }

  const user = currentUser.data;
  return (
    <Card className="border-primary/30 mb-5 border-2 p-4">
      <div className="flex gap-3">
        <Avatar
          name={user ? `${user.firstName} ${user.lastName}` : '?'}
          profileImage={user?.profileImage}
        />
        <div className="min-w-0 flex-1">
          <label htmlFor="banter-text" className="sr-only">
            What's on your mind?
          </label>
          <textarea
            id="banter-text"
            value={text}
            maxLength={BANTER_MAX_LENGTH}
            rows={2}
            onChange={(event) => setText(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="What's on your mind?"
            className="placeholder:text-muted-foreground w-full resize-none bg-transparent text-sm outline-none"
          />
          <div className="flex flex-wrap items-center gap-1.5">
            {text === ''
              ? PROMPTS.map((prompt) => (
                  <button
                    key={prompt}
                    type="button"
                    onClick={() => setText(`${prompt} `)}
                    className="bg-primary/10 text-primary hover:bg-primary/20 cursor-pointer rounded-full px-2.5 py-0.5 text-xs transition active:scale-95"
                  >
                    {prompt}
                  </button>
                ))
              : null}
            <span
              aria-live="polite"
              className={cn(
                'ml-auto text-xs tabular-nums',
                text.length >= COUNTER_WARNING_AT ? 'text-destructive' : 'text-muted-foreground',
              )}
            >
              {text.length > 0 ? remaining : null}
            </span>
            <Button
              type="button"
              className="h-8 px-3.5"
              onClick={submit}
              disabled={!trimmed || createBanter.isPending}
            >
              {createBanter.isPending ? (
                <LoaderCircle aria-hidden="true" className="animate-spin" />
              ) : (
                <Send aria-hidden="true" />
              )}
              Post
            </Button>
          </div>
          <div className="mt-2">
            <ContentError error={createBanter.error} fallback="Unable to post your banter." />
          </div>
        </div>
      </div>
    </Card>
  );
}
