import { X } from 'lucide-react';
import { useState, type KeyboardEvent } from 'react';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { MAX_TAGS, MAX_TAG_LENGTH, SUGGESTED_TAGS, normalizeTag } from '../lib/tags';

type TagPickerProps = {
  id?: string;
  value: string[];
  onChange: (next: string[]) => void;
  disabled?: boolean;
};

const pillClass = 'rounded-full border px-3 py-1 text-xs font-medium transition';

/** Tap a suggested topic or type your own; up to MAX_TAGS. */
export function TagPicker({ id, value, onChange, disabled = false }: TagPickerProps) {
  const [draft, setDraft] = useState('');
  const atLimit = value.length >= MAX_TAGS;
  const custom = value.filter((tag) => !(SUGGESTED_TAGS as readonly string[]).includes(tag));

  function toggle(tag: string) {
    if (value.includes(tag)) {
      onChange(value.filter((existing) => existing !== tag));
    } else if (!atLimit) {
      onChange([...value, tag]);
    }
  }

  function commitDraft() {
    const tag = normalizeTag(draft);
    setDraft('');
    if (!tag || atLimit || value.includes(tag)) return;
    onChange([...value, tag]);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      commitDraft();
    } else if (event.key === 'Backspace' && draft === '' && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div className="space-y-3">
      <fieldset className="flex min-w-0 flex-wrap gap-1.5">
        <legend className="sr-only">Suggested topics</legend>
        {SUGGESTED_TAGS.map((tag) => {
          const selected = value.includes(tag);
          return (
            <button
              key={tag}
              type="button"
              aria-pressed={selected}
              disabled={disabled || (!selected && atLimit)}
              onClick={() => toggle(tag)}
              className={cn(
                pillClass,
                'cursor-pointer active:scale-95 disabled:cursor-not-allowed disabled:opacity-50',
                selected
                  ? 'border-primary bg-primary text-primary-foreground'
                  : 'bg-card text-muted-foreground hover:border-primary/50 hover:text-foreground',
              )}
            >
              #{tag}
            </button>
          );
        })}
        {custom.map((tag) => (
          <span
            key={tag}
            className={cn(
              pillClass,
              'border-primary bg-primary text-primary-foreground flex items-center gap-1 pr-1.5',
            )}
          >
            #{tag}
            <button
              type="button"
              disabled={disabled}
              onClick={() => toggle(tag)}
              aria-label={`Remove ${tag}`}
              className="rounded-full p-0.5 hover:bg-white/20"
            >
              <X aria-hidden="true" className="size-3" />
            </button>
          </span>
        ))}
      </fieldset>
      <Input
        id={id}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={commitDraft}
        maxLength={MAX_TAG_LENGTH}
        disabled={disabled || atLimit}
        placeholder={atLimit ? `Maximum of ${MAX_TAGS} topics` : 'Add your own, then press Enter'}
      />
    </div>
  );
}
