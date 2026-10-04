import { Clock, GraduationCap } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';

// Real example invite posts from the product brief, with plausible poster
// details layered on top so this reads as an actual invite post rather
// than a floating quote. Every invite belongs to a campus.
const posts = [
  {
    name: 'Khadir',
    campus: 'SFSU',
    hoursLeft: 22,
    content: 'I just moved to San Francisco from The Gambia. Any Gambians here want to hang out?',
  },
  {
    name: 'Jordan',
    campus: 'SFSU',
    hoursLeft: 6,
    content: 'Wanna hang out next Saturday at the beach?',
  },
  {
    name: 'Priya',
    campus: 'SFSU',
    hoursLeft: 14,
    content: 'Anyone studying Java at SFSU tonight?',
  },
] as const;

/** A few example invites shown on the signed-out landing page. */
export function SampleInvites() {
  return (
    <div className="mt-10">
      <p className="mb-3 flex items-center gap-2 text-sm font-medium">
        <span className="relative flex size-2">
          <span className="bg-primary absolute inline-flex size-full animate-ping rounded-full opacity-75" />
          <span className="bg-primary relative inline-flex size-2 rounded-full" />
        </span>
        People are already posting
      </p>
      <div className="space-y-2">
        {posts.map((post, index) => (
          <div
            key={post.name}
            className="animate-fade-up bg-card flex items-center gap-3 rounded-2xl border p-3 transition hover:-translate-y-0.5 hover:shadow-md"
            style={{ animationDelay: `${160 + index * 80}ms` }}
          >
            <Avatar name={post.name} className="size-8 text-xs" />
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-semibold">{post.content}</div>
              <div className="text-muted-foreground flex gap-3 truncate text-xs">
                <span className="flex items-center gap-1">
                  <GraduationCap aria-hidden="true" className="size-3" />
                  {post.campus}
                </span>
                <span className="flex items-center gap-1">
                  <Clock aria-hidden="true" className="size-3" />
                  {post.hoursLeft}h left
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>
      <p className="text-muted-foreground mt-3 text-center text-xs">
        Sign up to see them all and ask to join.
      </p>
    </div>
  );
}
