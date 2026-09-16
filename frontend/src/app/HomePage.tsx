/**
 * Placeholder home route for the foundation branch — proves the router,
 * layout shell, and Tailwind/shadcn tokens render correctly. Replaced by the
 * real feed once feature/frontend-invite-posts lands.
 */
export function HomePage() {
  return (
    <div className="flex flex-col gap-2">
      <h1 className="text-foreground text-3xl font-semibold">OpenCircle</h1>
      <p className="text-muted-foreground">
        Location-based invites and conversations without the follower system.
      </p>
    </div>
  );
}
