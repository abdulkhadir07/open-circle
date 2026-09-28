import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { annualAward, authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { HallOfFamePage } from './HallOfFamePage';

function renderHallOfFamePage() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<HallOfFamePage />, { queryClient });
}

const LATEST_POSSIBLE_YEAR = new Date().getUTCFullYear() - 1;

describe('HallOfFamePage', () => {
  it("shows the previous year's winner by default", async () => {
    server.use(http.get('*/api/awards/:year', () => HttpResponse.json(annualAward)));

    renderHallOfFamePage();

    expect(await screen.findByText(authUser.username)).toBeInTheDocument();
    expect(screen.getByText('210 Circle Points')).toBeInTheDocument();
    expect(screen.getByText(String(LATEST_POSSIBLE_YEAR))).toBeInTheDocument();
  });

  it('shows every winner on a tied season', async () => {
    const tiedAward = {
      ...annualAward,
      winners: [
        { userId: 'a', username: 'racer_a', profileImage: null, finalScore: 100 },
        { userId: 'b', username: 'racer_b', profileImage: null, finalScore: 100 },
      ],
    };
    server.use(http.get('*/api/awards/:year', () => HttpResponse.json(tiedAward)));

    renderHallOfFamePage();

    expect(await screen.findByText('racer_a')).toBeInTheDocument();
    expect(screen.getByText('racer_b')).toBeInTheDocument();
  });

  it('shows an empty state when the season was finalized with no winners', async () => {
    server.use(
      http.get('*/api/awards/:year', () => HttpResponse.json({ ...annualAward, winners: [] })),
    );

    renderHallOfFamePage();

    expect(
      await screen.findByText(
        `No one scored enough to win Circle Champion ${LATEST_POSSIBLE_YEAR}.`,
      ),
    ).toBeInTheDocument();
  });

  it('shows a not-yet-crowned empty state on a 404, not a hard error', async () => {
    server.use(
      http.get('*/api/awards/:year', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 404,
            error: 'NOT_FOUND',
            message: `Annual award has not been finalized for ${LATEST_POSSIBLE_YEAR}`,
            path: `/api/awards/${LATEST_POSSIBLE_YEAR}`,
            fieldErrors: {},
          },
          { status: 404 },
        ),
      ),
    );

    renderHallOfFamePage();

    expect(
      await screen.findByText(
        `No Circle Champion has been crowned for ${LATEST_POSSIBLE_YEAR} yet.`,
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows an error message for a non-404 failure', async () => {
    server.use(
      http.get('*/api/awards/:year', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Unable to load this award right now',
            path: `/api/awards/${LATEST_POSSIBLE_YEAR}`,
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );

    renderHallOfFamePage();

    expect(await screen.findByText('Unable to load this award right now')).toBeInTheDocument();
  });

  it('navigates a year back and forward, disabling Next at the latest possible year', async () => {
    server.use(http.get('*/api/awards/:year', () => HttpResponse.json(annualAward)));
    const { user } = renderHallOfFamePage();

    await screen.findByText(authUser.username);
    expect(screen.getByRole('button', { name: 'Next year' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Previous year' }));

    expect(await screen.findByText(String(LATEST_POSSIBLE_YEAR - 1))).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next year' })).toBeEnabled();
  });
});
