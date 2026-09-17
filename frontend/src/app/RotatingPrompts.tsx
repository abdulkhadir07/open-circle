import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { Clock, MapPin, Quote } from 'lucide-react';
import { useEffect, useState } from 'react';

// Real example invite posts from the product brief, with plausible poster
// details layered on top so this reads as an actual invite post rather
// than a floating quote. Location matches the brief's own worked example
// ("a user in San Francisco, California, United States").
const posts = [
  {
    name: 'Fatou',
    location: 'San Francisco, CA',
    hoursLeft: 22,
    content: 'I just moved to San Francisco from The Gambia. Any Gambians here want to hang out?',
  },
  {
    name: 'Jordan',
    location: 'San Francisco, CA',
    hoursLeft: 6,
    content: 'Wanna hang out next Saturday at the beach?',
  },
  {
    name: 'Priya',
    location: 'San Francisco, CA',
    hoursLeft: 14,
    content: 'Anyone studying Java at SFSU tonight?',
  },
] as const;

const cardShape = 'rounded-tl-[2.75rem] rounded-tr-lg rounded-br-[2.75rem] rounded-bl-lg';

export function RotatingPrompts() {
  const [index, setIndex] = useState(0);
  const reduceMotion = useReducedMotion();

  useEffect(() => {
    const timer = window.setInterval(() => {
      setIndex((value) => (value + 1) % posts.length);
    }, 4200);
    return () => window.clearInterval(timer);
  }, []);

  const post = posts[index] ?? posts[0];

  return (
    <div className="relative">
      {/* Ghost cards behind the active one, same silhouette, purely decorative. */}
      <div
        aria-hidden="true"
        className={`border-primary/10 bg-card absolute inset-x-6 top-4 h-full -rotate-2 border opacity-30 ${cardShape}`}
      />
      <div
        aria-hidden="true"
        className={`border-primary/10 bg-card absolute inset-x-3 top-2 h-full rotate-1 border opacity-50 ${cardShape}`}
      />

      <div
        className={`from-card to-primary/[0.07] border-primary/15 relative min-h-64 overflow-hidden border bg-gradient-to-br p-7 shadow-lg ${cardShape}`}
      >
        <Quote
          aria-hidden="true"
          className="text-primary/10 pointer-events-none absolute -top-3 -right-3 size-28 rotate-12"
          strokeWidth={1}
        />

        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={index}
            initial={reduceMotion ? false : { opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={reduceMotion ? undefined : { opacity: 0, y: -10 }}
            transition={{ duration: reduceMotion ? 0 : 0.35, ease: 'easeOut' }}
            className="relative flex h-full flex-col justify-between"
          >
            <div>
              <div className="mb-4 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <span className="bg-accent/20 text-accent flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold">
                    {post.name[0]}
                  </span>
                  <div>
                    <p className="text-foreground text-sm font-semibold">{post.name}</p>
                    <p className="text-muted-foreground flex items-center gap-1 text-xs">
                      <MapPin aria-hidden="true" className="size-3" />
                      {post.location}
                    </p>
                  </div>
                </div>
                <span className="text-muted-foreground flex shrink-0 items-center gap-1 text-xs">
                  <Clock aria-hidden="true" className="size-3" />
                  {post.hoursLeft}h left
                </span>
              </div>

              <p className="text-foreground text-xl leading-7 font-medium text-balance">
                {post.content}
              </p>
            </div>

            <span className="bg-primary text-primary-foreground mt-5 inline-flex w-fit items-center rounded-full px-4 py-1.5 text-xs font-semibold">
              Engage
            </span>
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  );
}
