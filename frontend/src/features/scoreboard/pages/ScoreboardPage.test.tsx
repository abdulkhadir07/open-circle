import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, scoreboard, scoreSummary } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { ScoreboardPage } from './ScoreboardPage';

function renderScoreboardPage() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<ScoreboardPage />, { queryClient });
}

describe('ScoreboardPage', () => {
  it('shows an empty state when no one has scored yet', async () => {
    renderScoreboardPage();

    expect(await screen.findByText('No one has scored yet this season.')).toBeInTheDocument();
  });

  it("shows the current user's own score and season", async () => {
    server.use(http.get('*/api/users/me/score', () => HttpResponse.json(scoreSummary)));

    renderScoreboardPage();

    expect(await screen.findByText('42 Circle Points')).toBeInTheDocument();
    expect(screen.getByText('2026 season')).toBeInTheDocument();
    expect(screen.getByText('128 Circle Points')).toBeInTheDocument();
    expect(screen.getByText('Lifetime')).toBeInTheDocument();
  });

  it('shows an error message when the score fails to load', async () => {
    server.use(
      http.get('*/api/users/me/score', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Unable to load your score right now',
            path: '/api/users/me/score',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );

    renderScoreboardPage();

    expect(await screen.findByText('Unable to load your score right now')).toBeInTheDocument();
  });

  it('lists the top 5 entries, marking the current user', async () => {
    server.use(http.get('*/api/scoreboard', () => HttpResponse.json(scoreboard)));

    renderScoreboardPage();

    expect(await screen.findByText(authUser.username)).toBeInTheDocument();
    expect(screen.getByText('(you)')).toBeInTheDocument();
    expect(screen.getByText(scoreboard.entries[1]!.username)).toBeInTheDocument();
  });

  it('shows a podium for the top 3 and a divided list for the rest', async () => {
    const fullBoard = {
      seasonYear: 2026,
      entries: Array.from({ length: 5 }, (_, index) => ({
        rank: index + 1,
        userId: `user-${index + 1}`,
        username: `racer${index + 1}`,
        profileImage: null,
        annualScore: 50 - index * 10,
        averageRating: 4,
        currentYearDistinctRaterCount: 2,
      })),
    };
    server.use(http.get('*/api/scoreboard', () => HttpResponse.json(fullBoard)));

    renderScoreboardPage();

    for (const entry of fullBoard.entries) {
      expect(await screen.findByText(entry.username)).toBeInTheDocument();
    }
    // Ranks 4 and 5 get a numeric rank marker in the divided list; ranks 1-3 don't (podium instead).
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.queryByText('1')).not.toBeInTheDocument();
  });

  it('shows an error message when the scoreboard fails to load', async () => {
    server.use(
      http.get('*/api/scoreboard', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Unable to load the scoreboard right now',
            path: '/api/scoreboard',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );

    renderScoreboardPage();

    expect(await screen.findByText('Unable to load the scoreboard right now')).toBeInTheDocument();
  });
});
