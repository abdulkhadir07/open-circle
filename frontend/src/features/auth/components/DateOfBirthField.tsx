import { useCallback, useId, useLayoutEffect, useRef, useState } from 'react';
import { AnimatedError } from './AnimatedError';

const ROW_HEIGHT = 40;
const VISIBLE_ROWS = 5;
const PADDING_ROWS = Math.floor(VISIBLE_ROWS / 2);

const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

function daysInMonth(monthIndex: number, year: number) {
  return new Date(year, monthIndex + 1, 0).getDate();
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}

// Not every environment implements Element.scrollTo (jsdom doesn't), so this
// guards the call rather than assuming it's always present.
function smoothScrollTo(el: HTMLElement, top: number) {
  if (typeof el.scrollTo === 'function') {
    el.scrollTo({ top, behavior: 'smooth' });
  }
}

function parseIso(value: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return { year: null, month: null, day: null };
  const [, y, m, d] = match;
  return { year: Number(y), month: Number(m) - 1, day: Number(d) };
}

// --- One scrollable, snapping column (day, month, or year) ----------------

type WheelColumnProps = {
  ariaLabel: string;
  labels: string[];
  selectedIndex: number;
  onSelect: (index: number) => void;
};

function WheelColumn({ ariaLabel, labels, selectedIndex, onSelect }: WheelColumnProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [offset, setOffset] = useState(selectedIndex);
  const settleTimer = useRef<ReturnType<typeof window.setTimeout> | undefined>(undefined);
  const rafId = useRef<number | undefined>(undefined);
  const suppressSettle = useRef(false);

  // Keep scroll position in sync when the selection changes from outside
  // (initial mount, or the day count shrinking when the month changes).
  useLayoutEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const target = selectedIndex * ROW_HEIGHT;
    if (Math.abs(el.scrollTop - target) > 1) {
      suppressSettle.current = true;
      el.scrollTop = target;
      setOffset(selectedIndex);
      suppressSettle.current = false;
    }
  }, [selectedIndex]);

  function goTo(index: number) {
    const clamped = Math.min(Math.max(index, 0), labels.length - 1);
    onSelect(clamped);
    const el = containerRef.current;
    if (!el) return;
    smoothScrollTo(el, clamped * ROW_HEIGHT);
    // Clicking an option row doesn't reliably move DOM focus to this
    // container in every browser, which would silently break keyboard
    // navigation right after a click — focus it explicitly instead.
    el.focus();
  }

  const handleScroll = useCallback(() => {
    const el = containerRef.current;
    if (!el) return;

    if (rafId.current) cancelAnimationFrame(rafId.current);
    rafId.current = requestAnimationFrame(() => {
      setOffset(el.scrollTop / ROW_HEIGHT);
    });

    if (suppressSettle.current) return;

    window.clearTimeout(settleTimer.current);
    settleTimer.current = window.setTimeout(() => {
      const nearest = Math.min(
        Math.max(Math.round(el.scrollTop / ROW_HEIGHT), 0),
        labels.length - 1,
      );
      if (nearest !== selectedIndex) {
        onSelect(nearest);
      }
      smoothScrollTo(el, nearest * ROW_HEIGHT);
    }, 120);
  }, [labels.length, onSelect, selectedIndex]);

  return (
    <div
      ref={containerRef}
      role="listbox"
      aria-label={ariaLabel}
      aria-activedescendant={`${ariaLabel}-option-${selectedIndex}`}
      tabIndex={0}
      onScroll={handleScroll}
      onKeyDown={(event) => {
        if (event.key === 'ArrowDown') {
          event.preventDefault();
          goTo(selectedIndex + 1);
        } else if (event.key === 'ArrowUp') {
          event.preventDefault();
          goTo(selectedIndex - 1);
        } else if (event.key === 'Home') {
          event.preventDefault();
          goTo(0);
        } else if (event.key === 'End') {
          event.preventDefault();
          goTo(labels.length - 1);
        }
      }}
      className="focus-visible:ring-ring/40 snap-y snap-mandatory [scrollbar-width:none] overflow-y-auto rounded-lg outline-none focus-visible:ring-2 [&::-webkit-scrollbar]:hidden"
      style={{ height: ROW_HEIGHT * VISIBLE_ROWS }}
    >
      <div style={{ height: ROW_HEIGHT * PADDING_ROWS }} aria-hidden="true" />
      {labels.map((text, index) => {
        const distance = Math.abs(index - offset);
        const opacity = Math.max(0.18, 1 - distance * 0.32);
        const scale = Math.max(0.78, 1 - distance * 0.09);
        return (
          // Deliberate aria-activedescendant listbox pattern (WAI-ARIA APG):
          // the container keeps real DOM focus and handles all key input;
          // options are click/touch targets only, never individually
          // tabbable, so they don't need their own key listener or tabIndex.
          // oxlint-disable-next-line jsx-a11y/click-events-have-key-events jsx-a11y/interactive-supports-focus
          <div
            key={text || '—'}
            id={`${ariaLabel}-option-${index}`}
            role="option"
            aria-selected={index === selectedIndex}
            onClick={() => goTo(index)}
            className="text-foreground flex snap-center items-center justify-center text-lg font-medium select-none"
            style={{ height: ROW_HEIGHT, opacity, transform: `scale(${scale})` }}
          >
            {text || '—'}
          </div>
        );
      })}
      <div style={{ height: ROW_HEIGHT * PADDING_ROWS }} aria-hidden="true" />
    </div>
  );
}

// --- Combined day / month / year picker ------------------------------------

type DateOfBirthFieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
};

export function DateOfBirthField({ label, value, onChange, error }: DateOfBirthFieldProps) {
  const [initial] = useState(() => parseIso(value));
  const [year, setYear] = useState<number | null>(initial.year);
  const [month, setMonth] = useState<number | null>(initial.month);
  const [day, setDay] = useState<number | null>(initial.day);
  const [years] = useState(() => {
    const end = new Date().getFullYear();
    const start = end - 119;
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  });

  // Nothing is chosen for a blank field, so the day list can't yet know a
  // real month/year — fall back to a 31-day month so no real value gets
  // clamped away before the user has picked anything.
  const dayCount = daysInMonth(month ?? 0, year ?? 2000);
  const clampedDay = day === null ? null : Math.min(day, dayCount);

  useLayoutEffect(() => {
    if (year === null || month === null || clampedDay === null) {
      onChange('');
    } else {
      onChange(`${year}-${pad2(month + 1)}-${pad2(clampedDay)}`);
    }
  }, [year, month, clampedDay, onChange]);

  const id = useId();
  const errorId = error ? `${id}-error` : undefined;

  const dayLabels = ['', ...Array.from({ length: dayCount }, (_, i) => String(i + 1))];
  const monthLabels = ['', ...MONTH_NAMES];
  const yearLabels = ['', ...years.map(String)];

  return (
    <div className="space-y-2">
      <span className="text-foreground block text-sm font-medium">{label}</span>
      <div className="relative">
        <div
          aria-hidden="true"
          className="bg-muted/50 border-border pointer-events-none absolute inset-x-0 border-y"
          style={{ top: PADDING_ROWS * ROW_HEIGHT, height: ROW_HEIGHT }}
        />
        <div className="flex gap-1" aria-describedby={errorId}>
          <WheelColumn
            ariaLabel="Day"
            labels={dayLabels}
            selectedIndex={clampedDay === null ? 0 : clampedDay}
            onSelect={(index) => setDay(index === 0 ? null : index)}
          />
          <WheelColumn
            ariaLabel="Month"
            labels={monthLabels}
            selectedIndex={month === null ? 0 : month + 1}
            onSelect={(index) => setMonth(index === 0 ? null : index - 1)}
          />
          <WheelColumn
            ariaLabel="Year"
            labels={yearLabels}
            selectedIndex={year === null ? 0 : years.indexOf(year) + 1}
            onSelect={(index) => setYear(index === 0 ? null : (years[index - 1] ?? null))}
          />
        </div>
        <div className="from-card lg:from-background pointer-events-none absolute inset-x-0 top-0 h-8 bg-gradient-to-b to-transparent" />
        <div className="from-card lg:from-background pointer-events-none absolute inset-x-0 bottom-0 h-8 bg-gradient-to-t to-transparent" />
      </div>
      <AnimatedError id={errorId} message={error} />
    </div>
  );
}
