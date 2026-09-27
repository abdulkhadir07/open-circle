import { ChevronLeft, ChevronRight } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { useState } from 'react';
import { cn } from '@/lib/utils';
import type { InvitePostImage } from '../api/contracts';

export function PostImageCarousel({ images }: { images: InvitePostImage[] }) {
  const reduceMotion = useReducedMotion();
  const [index, setIndex] = useState(0);

  if (images.length === 0) return null;

  const ordered = [...images].sort((a, b) => a.displayOrder - b.displayOrder);
  const current = ordered[Math.min(index, ordered.length - 1)]!;

  if (ordered.length === 1) {
    return (
      <div className="bg-muted mt-4 aspect-[4/3] w-full overflow-hidden rounded-xl">
        <img src={current.url} alt="Attached to this invite" className="size-full object-cover" />
      </div>
    );
  }

  function goTo(next: number) {
    setIndex((next + ordered.length) % ordered.length);
  }

  return (
    <div className="bg-muted relative mt-4 aspect-[4/3] w-full overflow-hidden rounded-xl">
      <AnimatePresence mode="wait" initial={false}>
        <motion.img
          key={current.id}
          src={current.url}
          alt={`Attached to this invite, ${index + 1} of ${ordered.length}`}
          initial={reduceMotion ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={reduceMotion ? undefined : { opacity: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.2 }}
          className="absolute inset-0 size-full object-cover"
        />
      </AnimatePresence>

      <button
        type="button"
        onClick={() => goTo(index - 1)}
        aria-label="Previous photo"
        className="absolute top-1/2 left-2 flex size-7 -translate-y-1/2 items-center justify-center rounded-full bg-black/50 text-white hover:bg-black/70"
      >
        <ChevronLeft aria-hidden="true" className="size-4" />
      </button>
      <button
        type="button"
        onClick={() => goTo(index + 1)}
        aria-label="Next photo"
        className="absolute top-1/2 right-2 flex size-7 -translate-y-1/2 items-center justify-center rounded-full bg-black/50 text-white hover:bg-black/70"
      >
        <ChevronRight aria-hidden="true" className="size-4" />
      </button>

      <div className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1.5">
        {ordered.map((image, dotIndex) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setIndex(dotIndex)}
            aria-label={`Go to photo ${dotIndex + 1}`}
            aria-current={dotIndex === index}
            className={cn(
              'size-1.5 rounded-full transition-colors',
              dotIndex === index ? 'bg-white' : 'bg-white/50',
            )}
          />
        ))}
      </div>
    </div>
  );
}
