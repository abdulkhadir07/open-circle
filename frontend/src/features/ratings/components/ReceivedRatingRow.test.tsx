import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { receivedRating } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ReceivedRatingRow } from './ReceivedRatingRow';

describe('ReceivedRatingRow', () => {
  it("shows the rater's name and relative time", () => {
    renderWithProviders(<ReceivedRatingRow rating={receivedRating} />);

    expect(screen.getByText(receivedRating.raterUsername)).toBeInTheDocument();
    expect(screen.getByText('Just now')).toBeInTheDocument();
  });

  it('shows the numeric score out of 5', () => {
    renderWithProviders(<ReceivedRatingRow rating={receivedRating} />);

    expect(screen.getByText('5/5')).toBeInTheDocument();
  });

  it('reflects a lower score', () => {
    renderWithProviders(<ReceivedRatingRow rating={{ ...receivedRating, score: 2 }} />);

    expect(screen.getByText('2/5')).toBeInTheDocument();
  });

  it('labels the score for accessibility', () => {
    renderWithProviders(<ReceivedRatingRow rating={receivedRating} />);

    expect(screen.getByLabelText('5 out of 5')).toBeInTheDocument();
  });
});
