import { ChevronDown } from 'lucide-react';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { AnimatedError } from './AnimatedError';

type SearchableSelectFieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: string[];
  error?: string;
  disabled?: boolean;
  hint?: string;
  autoComplete?: string;
};

// A select-only combobox: typing filters the list, but the committed value
// only ever changes when an option is actually picked (click or Enter) —
// unlike a plain text input, there's no way to submit a value that isn't in
// `options`.
export function SearchableSelectField({
  id,
  label,
  value,
  onChange,
  options,
  error,
  disabled,
  hint,
  autoComplete,
}: SearchableSelectFieldProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [highlighted, setHighlighted] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const listboxId = `${id}-listbox`;
  const errorId = error ? `${id}-error` : undefined;
  const hintId = hint ? `${id}-hint` : undefined;

  // While closed, the field just shows the committed value; `query` only
  // exists to hold what's being typed while the list is open.
  const displayValue = open ? query : value;

  const filtered = query.trim()
    ? options.filter((option) => option.toLowerCase().includes(query.trim().toLowerCase()))
    : options;

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [open]);

  function commit(option: string) {
    onChange(option);
    setOpen(false);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!open) {
        setQuery('');
        setHighlighted(0);
        setOpen(true);
        return;
      }
      setHighlighted((index) => Math.min(index + 1, filtered.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlighted((index) => Math.max(index - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const option = filtered[highlighted];
      if (option) commit(option);
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <div className="space-y-2">
      <div className="relative" ref={containerRef}>
        <Input
          id={id}
          role="combobox"
          aria-expanded={open}
          aria-controls={listboxId}
          aria-activedescendant={
            open && filtered[highlighted] ? `${id}-option-${highlighted}` : undefined
          }
          aria-autocomplete="list"
          aria-invalid={Boolean(error)}
          aria-describedby={[errorId, hintId].filter(Boolean).join(' ') || undefined}
          autoComplete={autoComplete}
          disabled={disabled}
          placeholder=" "
          value={displayValue}
          onFocus={() => {
            setQuery('');
            setHighlighted(0);
            setOpen(true);
          }}
          onChange={(event) => {
            setQuery(event.target.value);
            setHighlighted(0);
            setOpen(true);
          }}
          onBlur={() => setOpen(false)}
          onKeyDown={handleKeyDown}
          className="peer h-14 pt-4 pr-10 pb-1"
        />
        <label
          htmlFor={id}
          className={cn(
            'text-muted-foreground pointer-events-none absolute top-1/2 left-3 -translate-y-1/2',
            'text-base transition-all duration-150 ease-out',
            'peer-focus:text-primary peer-focus:top-3.5 peer-focus:translate-y-0 peer-focus:text-xs',
            'peer-[:not(:placeholder-shown)]:top-3.5 peer-[:not(:placeholder-shown)]:translate-y-0 peer-[:not(:placeholder-shown)]:text-xs',
          )}
        >
          {label}
        </label>
        <ChevronDown
          aria-hidden="true"
          className={cn(
            'text-muted-foreground pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 transition-transform',
            open && 'rotate-180',
          )}
        />
        {open && !disabled ? (
          <div
            id={listboxId}
            role="listbox"
            aria-label={label}
            className="bg-card border-border absolute inset-x-0 top-full z-10 mt-1 max-h-60 overflow-y-auto rounded-md border py-1 shadow-lg"
          >
            {filtered.length ? (
              filtered.map((option, index) => (
                // Same deliberate aria-activedescendant listbox pattern as
                // DateOfBirthField's wheel columns: the input keeps real DOM
                // focus and handles all key input, so option rows are
                // click/touch targets only.
                // oxlint-disable-next-line jsx-a11y/click-events-have-key-events jsx-a11y/interactive-supports-focus
                <div
                  key={option}
                  id={`${id}-option-${index}`}
                  role="option"
                  aria-selected={option === value}
                  onMouseDown={(event) => {
                    event.preventDefault();
                    commit(option);
                  }}
                  onMouseEnter={() => setHighlighted(index)}
                  className={cn(
                    'cursor-pointer px-3 py-2 text-sm',
                    index === highlighted ? 'bg-primary/10 text-primary' : 'text-foreground',
                  )}
                >
                  {option}
                </div>
              ))
            ) : (
              <p className="text-muted-foreground px-3 py-2 text-sm">No matches</p>
            )}
          </div>
        ) : null}
      </div>
      {hint ? (
        <p id={hintId} className="text-muted-foreground text-xs leading-5">
          {hint}
        </p>
      ) : null}
      <AnimatedError id={errorId} message={error} />
    </div>
  );
}
