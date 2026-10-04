import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { StarRatingPicker } from './StarRatingPicker';

describe('StarRatingPicker', () => {
  it('offers five stars', () => {
    renderWithProviders(
      <StarRatingPicker engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    for (const value of [1, 2, 3, 4, 5]) {
      expect(screen.getByRole('button', { name: `${value} out of 5` })).toBeInTheDocument();
    }
  });

  it('submits the clicked score straight away and confirms it stays sealed', async () => {
    let submittedScore: number | undefined;
    server.use(
      http.post('*/api/engagements/:engagementId/ratings', async ({ request }) => {
        submittedScore = ((await request.json()) as { score: number }).score;
        return HttpResponse.json(
          {
            id: 'rating-id',
            engagementId: 'engagement-1',
            ratedUserId: 'user',
            score: submittedScore,
            submittedAt: new Date().toISOString(),
            revealed: false,
          },
          { status: 201 },
        );
      }),
    );
    const { user } = renderWithProviders(
      <StarRatingPicker engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: '4 out of 5' }));

    expect(
      await screen.findByText(/Thanks! Sealed until jordan.lee rates you back/),
    ).toBeInTheDocument();
    expect(submittedScore).toBe(4);
  });

  it('shows an error and lets the user try again when submission fails', async () => {
    server.use(
      http.post('*/api/engagements/:engagementId/ratings', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 409,
            error: 'CONFLICT',
            message: 'You already rated this person',
            path: '/api/engagements/engagement-1/ratings',
            fieldErrors: {},
          },
          { status: 409 },
        ),
      ),
    );
    const { user } = renderWithProviders(
      <StarRatingPicker engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: '5 out of 5' }));

    expect(await screen.findByText('You already rated this person')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '5 out of 5' })).toBeEnabled();
  });
});
