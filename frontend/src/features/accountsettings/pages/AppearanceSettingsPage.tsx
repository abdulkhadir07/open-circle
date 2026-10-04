import { Check } from 'lucide-react';
import { useState } from 'react';
import { applyTheme, getStoredTheme, type ThemeChoice } from '@/lib/theme';
import { cn } from '@/lib/utils';
import { SettingsSectionLayout } from '../components/SettingsSectionLayout';

const OPTIONS: { value: ThemeChoice; label: string; description: string }[] = [
  { value: 'system', label: 'System', description: 'Match your device' },
  { value: 'light', label: 'Light', description: 'Always light' },
  { value: 'dark', label: 'Dark', description: 'Always dark' },
];

/** A tiny, fixed-colour mock of the app so each tile shows its theme regardless of the current one. */
function ThemePreview({ value }: { value: ThemeChoice }) {
  const light = (
    <div className="h-full w-full bg-neutral-100 p-2">
      <div className="mb-1.5 h-2 w-8 rounded-full bg-violet-600" />
      <div className="mb-1 h-6 rounded-md border border-neutral-200 bg-white" />
      <div className="h-6 rounded-md border border-neutral-200 bg-white" />
    </div>
  );
  const dark = (
    <div className="h-full w-full bg-neutral-950 p-2">
      <div className="mb-1.5 h-2 w-8 rounded-full bg-violet-400" />
      <div className="mb-1 h-6 rounded-md border border-neutral-800 bg-neutral-900" />
      <div className="h-6 rounded-md border border-neutral-800 bg-neutral-900" />
    </div>
  );

  if (value === 'light') return light;
  if (value === 'dark') return dark;

  return (
    <div className="relative h-full w-full">
      {light}
      <div className="absolute inset-0 [clip-path:polygon(100%_0,100%_100%,0_100%)]">{dark}</div>
    </div>
  );
}

export function AppearanceSettingsPage() {
  const [choice, setChoice] = useState<ThemeChoice>(getStoredTheme);

  function select(next: ThemeChoice) {
    setChoice(next);
    applyTheme(next);
  }

  return (
    <SettingsSectionLayout title="Appearance">
      <fieldset>
        <legend className="text-muted-foreground mb-3 text-sm">
          Choose how OpenCircle looks. System follows your device&apos;s light or dark setting.
        </legend>
        <div className="grid grid-cols-3 gap-3">
          {OPTIONS.map((option) => {
            const selected = option.value === choice;
            return (
              <label
                key={option.value}
                className={cn(
                  'bg-card focus-within:ring-primary/40 relative cursor-pointer rounded-2xl border p-2 transition focus-within:ring-2',
                  selected ? 'border-primary ring-primary/30 ring-2' : 'hover:border-primary/40',
                )}
              >
                <input
                  type="radio"
                  name="theme"
                  value={option.value}
                  checked={selected}
                  onChange={() => select(option.value)}
                  className="sr-only"
                />
                <div className="aspect-[4/3] overflow-hidden rounded-xl border">
                  <ThemePreview value={option.value} />
                </div>
                <div className="mt-2 flex items-center justify-between gap-2 px-1 pb-0.5">
                  <div className="min-w-0">
                    <div className="text-sm font-semibold">{option.label}</div>
                    <div className="text-muted-foreground truncate text-xs">
                      {option.description}
                    </div>
                  </div>
                  {selected ? (
                    <span className="bg-primary text-primary-foreground flex size-5 shrink-0 items-center justify-center rounded-full">
                      <Check aria-hidden="true" className="size-3" />
                    </span>
                  ) : null}
                </div>
              </label>
            );
          })}
        </div>
      </fieldset>
    </SettingsSectionLayout>
  );
}
