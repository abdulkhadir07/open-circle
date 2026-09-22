import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { dueRating } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { DueRatingRow } from './DueRatingRow';

describe('DueRatingRow', () => {
  it("shows the other user's name and the trigger reason", () => {
    renderWithProviders(<DueRatingRow dueRating={dueRating} />);

    expect(screen.getByText(dueRating.otherUsername)).toBeInTheDocument();
    expect(screen.getByText('You both left the chat')).toBeInTheDocument();
  });

  it('shows a countdown to the due deadline', () => {
    renderWithProviders(<DueRatingRow dueRating={dueRating} />);

    expect(screen.getByText(/left to rate/)).toBeInTheDocument();
  });

  it('shows Expired once the due deadline has passed', () => {
    renderWithProviders(
      <DueRatingRow
        dueRating={{ ...dueRating, dueAt: new Date(Date.now() - 1000).toISOString() }}
      />,
    );

    expect(screen.getByText('Expired')).toBeInTheDocument();
  });

  it('offers a Rate action', () => {
    renderWithProviders(<DueRatingRow dueRating={dueRating} />);

    expect(screen.getByRole('button', { name: 'Rate' })).toBeInTheDocument();
  });
});
