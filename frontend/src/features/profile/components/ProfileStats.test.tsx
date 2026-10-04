import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { userProfile } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ProfileStats } from './ProfileStats';

const base = {
  reputation: userProfile.reputation,
  awardsCount: 1,
  memberSince: userProfile.memberSince,
};

describe('ProfileStats', () => {
  it('shows the average rating, rating count, awards and member-since tiles', () => {
    renderWithProviders(<ProfileStats {...base} />);

    expect(screen.getByText('4.8/5')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('from 10 people')).toBeInTheDocument();
    expect(screen.getByText('Award')).toBeInTheDocument();
    expect(screen.getByText('Member since')).toBeInTheDocument();
    expect(screen.queryByText('Season points')).not.toBeInTheDocument();
    expect(screen.queryByText('Season rank')).not.toBeInTheDocument();
  });

  it('shows a dash when there are no ratings yet', () => {
    renderWithProviders(
      <ProfileStats
        {...base}
        reputation={{ averageRating: null, totalRatingsReceived: 0, distinctRaterCount: 0 }}
      />,
    );

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText(/from \d+ /)).not.toBeInTheDocument();
  });

  it('adds season points, and rank only when it is a number', () => {
    const { unmount } = renderWithProviders(<ProfileStats {...base} points={42} rank={null} />);

    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Season points')).toBeInTheDocument();
    expect(screen.queryByText('Season rank')).not.toBeInTheDocument();
    unmount();

    renderWithProviders(<ProfileStats {...base} points={42} rank={7} />);
    expect(screen.getByText('#7')).toBeInTheDocument();
    expect(screen.getByText('Season rank')).toBeInTheDocument();
  });
});
