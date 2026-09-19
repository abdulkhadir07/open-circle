import { ArrowLeft } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { ReceivedRequestsList } from '../components/ReceivedRequestsList';
import { SentRequestsList } from '../components/SentRequestsList';

const TAB_OPTIONS = [
  { value: 'received', label: 'Received' },
  { value: 'sent', label: 'Sent' },
] as const;

type Tab = (typeof TAB_OPTIONS)[number]['value'];

export function EngagementRequestsPage() {
  const reduceMotion = useReducedMotion();
  const [tab, setTab] = useState<Tab>('received');

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/"
        className="text-muted-foreground hover:text-foreground flex items-center gap-1.5 text-base font-medium"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back
      </Link>

      <p className="text-primary mt-4 text-sm font-semibold">Your circle</p>
      <h1 className="text-foreground text-3xl font-semibold">Requests</h1>

      <div
        role="radiogroup"
        aria-label="Requests"
        className="border-border mt-6 flex items-baseline gap-7 border-b"
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
                'relative pb-3 text-xl font-semibold tracking-tight transition-colors',
                selected
                  ? 'text-foreground'
                  : 'text-muted-foreground/60 hover:text-muted-foreground',
              )}
            >
              {option.label}
              {selected ? (
                <motion.span
                  layoutId="requests-tab-underline"
                  className="bg-primary absolute inset-x-0 bottom-0 h-[2.5px] rounded-full"
                  transition={
                    reduceMotion ? { duration: 0 } : { duration: 0.35, ease: [0.22, 1, 0.36, 1] }
                  }
                />
              ) : null}
            </button>
          );
        })}
      </div>

      <div className="mt-6">
        {tab === 'received' ? <ReceivedRequestsList /> : <SentRequestsList />}
      </div>
    </div>
  );
}
