import { motion, useReducedMotion } from 'motion/react';
import { Circle } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import authCommunity from '@/assets/auth-community.webp';

type AuthLayoutProps = {
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
};

export function AuthLayout({ title, description, children, footer }: AuthLayoutProps) {
  const reduceMotion = useReducedMotion();

  return (
    <main className="bg-muted/60 lg:bg-background min-h-svh p-3 sm:p-5 lg:grid lg:min-h-svh lg:grid-cols-[minmax(22rem,42%)_1fr] lg:p-0">
      <div className="mx-auto w-full max-w-5xl lg:contents">
        <div className="relative h-40 overflow-hidden rounded-2xl shadow-md sm:h-52 lg:h-svh lg:rounded-none lg:shadow-none">
          <img
            src={authCommunity}
            alt="Friends making plans together in a city plaza"
            className="h-full w-full object-cover object-[center_58%] lg:object-center"
          />
          <div className="bg-primary/95 absolute bottom-0 left-0 hidden max-w-sm p-8 text-white lg:block">
            <p className="text-xl leading-7 font-medium tracking-tight">
              A good plan starts with the right people.
            </p>
          </div>
        </div>

        <div className="bg-card lg:bg-background mt-3 flex min-h-[calc(100svh-11rem)] items-start justify-center rounded-2xl px-5 py-8 shadow-sm sm:min-h-[calc(100svh-13rem)] sm:px-8 sm:py-12 lg:mt-0 lg:min-h-svh lg:items-center lg:rounded-none lg:px-14 lg:py-16 lg:shadow-none">
          <motion.div
            data-slot="auth-content"
            initial={reduceMotion ? false : { opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.32, ease: 'easeOut' }}
            className="w-full max-w-md"
          >
            <Link to="/" className="mb-9 flex w-fit items-center gap-2">
              <Circle aria-hidden="true" className="text-primary size-5" strokeWidth={2.5} />
              <span className="text-primary text-lg font-semibold">OpenCircle</span>
            </Link>
            <header className="mb-8 space-y-2">
              <h1 className="text-foreground text-4xl font-semibold tracking-tight sm:text-5xl">
                {title}
              </h1>
              <p className="text-muted-foreground max-w-prose leading-6">{description}</p>
            </header>
            {children}
            {footer ? <div className="text-muted-foreground mt-8 text-sm">{footer}</div> : null}
          </motion.div>
        </div>
      </div>
    </main>
  );
}
