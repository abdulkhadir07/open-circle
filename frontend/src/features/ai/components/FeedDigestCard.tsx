import { Sparkles } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { AiBadge } from './AiBadge';

/** A one-glance summary of what's open near you today, above the feed. */
export function FeedDigestCard({ digest, aiGenerated }: { digest: string; aiGenerated: boolean }) {
  return (
    <Card className="border-primary/30 bg-primary/5 animate-fade-up mb-6 flex items-start gap-3 p-4">
      <Sparkles aria-hidden="true" className="text-primary mt-0.5 size-5 shrink-0" />
      <div className="min-w-0">
        <p className="text-sm leading-relaxed">{digest}</p>
        {aiGenerated ? <AiBadge className="mt-1.5" /> : null}
      </div>
    </Card>
  );
}
