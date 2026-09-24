import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { userProfile } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ProfileAwardsList } from './ProfileAwardsList';

describe('ProfileAwardsList', () => {
  it('shows a no-awards message when the list is empty', () => {
    renderWithProviders(<ProfileAwardsList awards={[]} />);

    expect(screen.getByText('No awards yet.')).toBeInTheDocument();
  });

  it('shows the award name and its score in Circle Points', () => {
    renderWithProviders(<ProfileAwardsList awards={userProfile.awards} />);

    expect(screen.getByText('Circle Champion 2025')).toBeInTheDocument();
    expect(screen.getByText('210 Circle Points')).toBeInTheDocument();
  });

  it('renders every award it is given', () => {
    const awards = [
      ...userProfile.awards,
      {
        seasonYear: 2024,
        name: 'Circle Champion 2024',
        finalScore: 90,
        awardedAt: '2025-01-01T00:05:00Z',
      },
    ];
    renderWithProviders(<ProfileAwardsList awards={awards} />);

    expect(screen.getByText('Circle Champion 2025')).toBeInTheDocument();
    expect(screen.getByText('Circle Champion 2024')).toBeInTheDocument();
  });
});
