import { ArrowLeft, LoaderCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ScoreboardPodium } from '../components/ScoreboardPodium';
import { ScoreboardRow } from '../components/ScoreboardRow';
import { useMyScore } from '../hooks/useMyScore';
import { useScoreboard } from '../hooks/useScoreboard';

export function ScoreboardPage() {
  const currentUser = useCurrentUser();
  const myScore = useMyScore();
  const scoreboard = useScoreboard();

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
      <h1 className="text-foreground text-3xl font-semibold">Scoreboard</h1>

      {myScore.isLoading ? (
        <LoaderCircle
          aria-hidden="true"
          className="text-muted-foreground mx-auto mt-4 block size-5 animate-spin"
        />
      ) : myScore.isError ? (
        <p role="alert" className="text-destructive mt-4 text-base">
          {myScore.error instanceof Error ? myScore.error.message : 'Unable to load your score.'}
        </p>
      ) : myScore.data ? (
        <div className="border-border bg-card mt-4 flex items-center justify-between gap-4 rounded-xl border p-4">
          <div>
            <p className="text-foreground text-2xl font-semibold tabular-nums">
              {myScore.data.annualScore} Circle Points
            </p>
            <p className="text-muted-foreground text-sm">{myScore.data.seasonYear} season</p>
          </div>
          <div className="text-right">
            <p className="text-foreground text-base font-semibold tabular-nums">
              {myScore.data.lifetimeScore} Circle Points
            </p>
            <p className="text-muted-foreground text-sm">Lifetime</p>
          </div>
        </div>
      ) : null}

      <h2 className="text-foreground mt-8 text-xl font-semibold">Top 5 this season</h2>

      <div className="mt-4">
        {scoreboard.isLoading ? (
          <LoaderCircle
            aria-hidden="true"
            className="text-muted-foreground mx-auto block size-5 animate-spin"
          />
        ) : scoreboard.isError ? (
          <p role="alert" className="text-destructive text-base">
            {scoreboard.error instanceof Error
              ? scoreboard.error.message
              : 'Unable to load the scoreboard.'}
          </p>
        ) : !scoreboard.data || scoreboard.data.entries.length === 0 ? (
          <p className="text-muted-foreground text-base">No one has scored yet this season.</p>
        ) : (
          <>
            <ScoreboardPodium
              entries={scoreboard.data.entries.slice(0, 3)}
              currentUserId={currentUser.data?.id}
            />
            {scoreboard.data.entries.length > 3 ? (
              <ul className="border-border bg-card divide-border mt-4 divide-y overflow-hidden rounded-xl border">
                {scoreboard.data.entries.slice(3).map((entry, index) => (
                  <ScoreboardRow
                    key={entry.userId}
                    entry={entry}
                    position={index + 4}
                    isCurrentUser={entry.userId === currentUser.data?.id}
                    scoreFraction={
                      scoreboard.data.entries[0]!.annualScore > 0
                        ? entry.annualScore / scoreboard.data.entries[0]!.annualScore
                        : 0
                    }
                  />
                ))}
              </ul>
            ) : null}
          </>
        )}
      </div>
    </div>
  );
}
