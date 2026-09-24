import { X } from 'lucide-react';
import { useState, type KeyboardEvent } from 'react';
import { Input } from '@/components/ui/input';
import { MAX_INTERESTS, MAX_INTEREST_LENGTH } from '../schemas/updateProfileSchema';

type InterestChipsProps = {
  id?: string;
  value: string[];
  onChange: (next: string[]) => void;
};

export function InterestChips({ id, value, onChange }: InterestChipsProps) {
  const [draft, setDraft] = useState('');
  const atLimit = value.length >= MAX_INTERESTS;

  function commitDraft() {
    const trimmed = draft.trim();
    setDraft('');
    if (!trimmed || atLimit) return;

    const alreadyPresent = value.some(
      (interest) => interest.toLowerCase() === trimmed.toLowerCase(),
    );
    if (!alreadyPresent) {
      onChange([...value, trimmed]);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      commitDraft();
    } else if (event.key === 'Backspace' && draft === '' && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  function removeInterest(interest: string) {
    onChange(value.filter((existing) => existing !== interest));
  }

  return (
    <div className="space-y-2">
      {value.length > 0 ? (
        <div className="flex flex-wrap gap-1.5">
          {value.map((interest) => (
            <span
              key={interest}
              className="bg-muted text-foreground flex items-center gap-1 rounded-full py-1 pr-1.5 pl-2.5 text-sm"
            >
              {interest}
              <button
                type="button"
                onClick={() => removeInterest(interest)}
                aria-label={`Remove ${interest}`}
                className="text-muted-foreground hover:text-foreground rounded-full p-0.5"
              >
                <X aria-hidden="true" className="size-3.5" />
              </button>
            </span>
          ))}
        </div>
      ) : null}
      <Input
        id={id}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={commitDraft}
        maxLength={MAX_INTEREST_LENGTH}
        disabled={atLimit}
        placeholder={atLimit ? 'Maximum of 8 interests' : 'Type an interest and press Enter'}
      />
    </div>
  );
}
