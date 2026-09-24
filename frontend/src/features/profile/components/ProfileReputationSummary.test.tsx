import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { ProfileReputationSummary } from './ProfileReputationSummary';

describe('ProfileReputationSummary', () => {
  it('shows a no-ratings message when nothing has been received', () => {
    renderWithProviders(
      <ProfileReputationSummary
        reputation={{ averageRating: null, totalRatingsReceived: 0, distinctRaterCount: 0 }}
      />,
    );

    expect(screen.getByText('No ratings yet.')).toBeInTheDocument();
  });

  it('shows the average out of 5 and the rater/rating counts', () => {
    renderWithProviders(
      <ProfileReputationSummary
        reputation={{ averageRating: 4.8, totalRatingsReceived: 12, distinctRaterCount: 10 }}
      />,
    );

    expect(screen.getByText('4.8/5')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText(/Ratings from 10 people/)).toBeInTheDocument();
  });

  it('singularizes rating/person counts of exactly one', () => {
    renderWithProviders(
      <ProfileReputationSummary
        reputation={{ averageRating: 5, totalRatingsReceived: 1, distinctRaterCount: 1 }}
      />,
    );

    expect(screen.getByText(/Rating from 1 person/)).toBeInTheDocument();
  });

  it('shows a whole-number average without a trailing decimal', () => {
    renderWithProviders(
      <ProfileReputationSummary
        reputation={{ averageRating: 5, totalRatingsReceived: 3, distinctRaterCount: 2 }}
      />,
    );

    expect(screen.getByText('5/5')).toBeInTheDocument();
  });
});
