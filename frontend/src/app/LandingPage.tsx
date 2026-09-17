import { motion, useReducedMotion } from 'motion/react';
import { ArrowRight, Circle, MessageCircle, SendHorizontal, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import authCommunity from '@/assets/auth-community.webp';
import { Button } from '@/components/ui/button';
import { RotatingPrompts } from './RotatingPrompts';

const steps = [
  {
    icon: SendHorizontal,
    title: 'Post what you’re up to',
    body: 'A beach trip, a coffee run, a study session — share a plan and choose who sees it: your city, your region, or everyone.',
  },
  {
    icon: Users,
    title: 'Someone nearby says yes',
    body: 'People who match your visibility can ask to join. No profile to build first — you just decide who’s in.',
  },
  {
    icon: MessageCircle,
    title: 'You’re in a chat',
    body: 'Accept a request and you’re both moved into a chat room to sort out the details.',
  },
] as const;

function fadeUp(reduceMotion: boolean | null, delay = 0) {
  return {
    initial: reduceMotion ? false : { opacity: 0, y: 16 },
    animate: { opacity: 1, y: 0 },
    transition: {
      duration: reduceMotion ? 0 : 0.5,
      delay: reduceMotion ? 0 : delay,
      ease: 'easeOut' as const,
    },
  };
}

export function LandingPage() {
  const reduceMotion = useReducedMotion();

  return (
    <div className="bg-background min-h-svh">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-5 py-6 sm:px-8">
        <div className="flex items-center gap-2">
          <Circle aria-hidden="true" className="text-primary size-5" strokeWidth={2.5} />
          <span className="text-foreground text-lg font-semibold">OpenCircle</span>
        </div>
        <Link to="/login" className="text-foreground text-sm font-semibold hover:underline">
          Log in
        </Link>
      </header>

      <section className="mx-auto grid w-full max-w-6xl gap-10 px-5 pt-6 pb-16 sm:px-8 sm:pt-10 sm:pb-24 lg:grid-cols-[1.1fr_1fr] lg:items-center lg:gap-16 lg:pt-16">
        <div>
          <motion.p
            {...fadeUp(reduceMotion)}
            className="text-primary mb-4 text-sm font-semibold tracking-wide uppercase"
          >
            Location-based &middot; not dating
          </motion.p>
          <motion.h1
            {...fadeUp(reduceMotion, 0.05)}
            className="text-foreground text-5xl font-semibold tracking-tight text-balance sm:text-6xl"
          >
            No followers.
            <br />
            Just people nearby.
          </motion.h1>
          <motion.p
            {...fadeUp(reduceMotion, 0.1)}
            className="text-muted-foreground mt-5 max-w-md text-lg leading-7"
          >
            OpenCircle is where people post what they&rsquo;re doing today &mdash; grabbing coffee,
            going to the beach, finding a study buddy &mdash; and anyone nearby can say yes. No
            profile to build, no one to follow first.
          </motion.p>
          <motion.div
            {...fadeUp(reduceMotion, 0.15)}
            className="mt-8 flex flex-wrap items-center gap-4"
          >
            <Button asChild size="lg" className="h-12 px-6 text-base active:scale-[0.98]">
              <Link to="/signup">
                Create an account
                <ArrowRight aria-hidden="true" />
              </Link>
            </Button>
            <Button asChild variant="ghost" size="lg" className="h-12 px-6 text-base">
              <Link to="/login">Log in</Link>
            </Button>
          </motion.div>
        </div>

        <motion.div
          initial={reduceMotion ? false : { opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: reduceMotion ? 0 : 0.6, ease: 'easeOut' }}
          className="relative"
        >
          <motion.div
            animate={reduceMotion ? undefined : { y: [0, -10, 0] }}
            transition={
              reduceMotion ? undefined : { duration: 6, repeat: Infinity, ease: 'easeInOut' }
            }
            className="overflow-hidden rounded-3xl shadow-lg"
          >
            <img
              src={authCommunity}
              alt="Friends making plans together in a city plaza"
              className="h-72 w-full object-cover sm:h-96 lg:h-[30rem]"
            />
          </motion.div>
        </motion.div>
      </section>

      <section className="mx-auto w-full max-w-3xl px-5 pb-16 sm:px-8 sm:pb-24">
        <motion.p
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: reduceMotion ? 0 : 0.4 }}
          className="text-muted-foreground mb-4 text-center text-sm font-semibold tracking-wide uppercase"
        >
          People are already posting
        </motion.p>
        <RotatingPrompts />
      </section>

      <section className="mx-auto w-full max-w-5xl px-5 pb-20 sm:px-8 sm:pb-28">
        <motion.h2
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: reduceMotion ? 0 : 0.4 }}
          className="text-foreground mb-10 text-center text-3xl font-semibold tracking-tight sm:text-4xl"
        >
          How it works
        </motion.h2>
        <div className="grid gap-6 sm:grid-cols-3">
          {steps.map((step, i) => (
            <motion.div
              key={step.title}
              initial={reduceMotion ? false : { opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-60px' }}
              transition={{ duration: reduceMotion ? 0 : 0.4, delay: reduceMotion ? 0 : i * 0.1 }}
              className="border-border bg-card rounded-2xl border p-6 shadow-sm"
            >
              <div className="bg-primary/10 text-primary mb-4 flex size-10 items-center justify-center rounded-full">
                <step.icon aria-hidden="true" className="size-5" />
              </div>
              <h3 className="text-foreground mb-2 text-lg font-semibold">{step.title}</h3>
              <p className="text-muted-foreground text-sm leading-6">{step.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="bg-primary">
        <div className="mx-auto flex w-full max-w-5xl flex-col items-center gap-6 px-5 py-16 text-center sm:px-8 sm:py-20">
          <h2 className="text-primary-foreground text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
            Your city already has plans happening.
          </h2>
          <Button
            asChild
            size="lg"
            variant="secondary"
            className="h-12 px-7 text-base active:scale-[0.98]"
          >
            <Link to="/signup">
              Create an account
              <ArrowRight aria-hidden="true" />
            </Link>
          </Button>
        </div>
      </section>

      <footer className="mx-auto flex w-full max-w-6xl items-center justify-center px-5 py-10 sm:justify-start">
        <div className="flex items-center gap-2">
          <Circle aria-hidden="true" className="text-primary size-4" strokeWidth={2.5} />
          <span className="text-muted-foreground text-sm font-medium">OpenCircle</span>
        </div>
      </footer>
    </div>
  );
}
