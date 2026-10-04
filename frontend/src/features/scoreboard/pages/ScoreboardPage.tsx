import { LoaderCircle, Trophy } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { PageHeader } from '@/components/ui/page-header';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ScoreboardPodium } from '../components/ScoreboardPodium';
import { ScoreboardRow } from '../components/ScoreboardRow';
import { useMyScore } from '../hooks/useMyScore';
import { useScoreboard } from '../hooks/useScoreboard';

export function ScoreboardPage() {
  const currentUser = useCurrentUser();
  const myScore = useMyScore();
  const scoreboard = useScoreboard();
  // `rank` on the score summary covers any position; fall back to the top-5 board for
  // older backends that don't send it.
  const myRank =
    myScore.data?.rank ??
    scoreboard.data?.entries.find((entry) => entry.userId === currentUser.data?.id)?.rank;

  return (
    <div>
      <div className="flex items-start justify-between gap-4">
        <PageHeader title="Scoreboard" sub="Host, join and rate people to climb the board." />
        <Link
          to="/scoreboard/hall-of-fame"
          className="text-primary shrink-0 pt-2 text-sm font-medium hover:underline"
        >
          Past winners
        </Link>
      </div>

      {myScore.isLoading ? (
        <LoaderCircle
          aria-hidden="true"
          className="text-muted-foreground mx-auto mb-6 block size-5 animate-spin"
        />
      ) : myScore.isError ? (
        <p role="alert" className="text-destructive mb-6 text-base">
          {myScore.error instanceof Error ? myScore.error.message : 'Unable to load your score.'}
        </p>
      ) : myScore.data ? (
        <Card className="bg-primary text-primary-foreground mb-6 flex items-center gap-4">
          <Trophy aria-hidden="true" className="size-8" />
          <div className="flex-1">
            <div className="text-sm opacity-80">Your circle score</div>
            <div className="text-3xl font-bold tabular-nums">
              {myScore.data.annualScore} Circle Points
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm opacity-80">Rank</div>
            <div className="text-2xl font-bold tabular-nums">
              {myRank ? `#${myRank}` : 'Unranked'}
            </div>
          </div>
        </Card>
      ) : null}

      <h2 className="mb-4 text-xl font-semibold">Top 5 this season</h2>

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
        <EmptyState icon={Trophy}>No one has scored yet this season.</EmptyState>
      ) : (
        <>
          <ScoreboardPodium
            entries={scoreboard.data.entries.slice(0, 3)}
            currentUserId={currentUser.data?.id}
          />
          {scoreboard.data.entries.length > 3 ? (
            <ul className="space-y-2">
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
  );
}
