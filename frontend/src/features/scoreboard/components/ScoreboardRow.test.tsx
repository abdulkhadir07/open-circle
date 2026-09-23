import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { scoreboard } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ScoreboardRow } from './ScoreboardRow';

describe('ScoreboardRow', () => {
  const [topEntry] = scoreboard.entries;

  it("shows the row's list position, username, and score in Circle Points", () => {
    renderWithProviders(<ScoreboardRow entry={topEntry!} position={4} scoreFraction={1} />);

    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText(topEntry!.username)).toBeInTheDocument();
    expect(screen.getByText('42 Circle Points')).toBeInTheDocument();
  });

  it("shows the given list position, not the entry's raw (tie-affected) API rank", () => {
    // topEntry.rank is 1, but a tie elsewhere on the board could legitimately
    // place this same entry at list position 4 or 5 — the row must reflect
    // the position it's actually shown at, not the raw rank value.
    renderWithProviders(<ScoreboardRow entry={topEntry!} position={5} scoreFraction={1} />);

    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.queryByText('1')).not.toBeInTheDocument();
  });

  it('does not show the average rating', () => {
    renderWithProviders(<ScoreboardRow entry={topEntry!} position={4} scoreFraction={1} />);

    expect(screen.queryByText(/\/5/)).not.toBeInTheDocument();
    expect(screen.queryByText(/rater/)).not.toBeInTheDocument();
  });

  it('marks the current user when isCurrentUser is set', () => {
    renderWithProviders(
      <ScoreboardRow entry={topEntry!} position={4} isCurrentUser scoreFraction={1} />,
    );

    expect(screen.getByText('(you)')).toBeInTheDocument();
  });

  it('does not mark other entries as the current user', () => {
    renderWithProviders(<ScoreboardRow entry={topEntry!} position={4} scoreFraction={1} />);

    expect(screen.queryByText('(you)')).not.toBeInTheDocument();
  });

  it('fills the score bar proportionally to scoreFraction', () => {
    const { container } = renderWithProviders(
      <ScoreboardRow entry={topEntry!} position={4} scoreFraction={0.5} />,
    );

    const fill = container.querySelector('[style*="width"]');
    expect(fill).toHaveStyle({ width: '50%' });
  });

  it('clamps an out-of-range scoreFraction into 0-1', () => {
    const { container } = renderWithProviders(
      <ScoreboardRow entry={topEntry!} position={4} scoreFraction={1.5} />,
    );

    const fill = container.querySelector('[style*="width"]');
    expect(fill).toHaveStyle({ width: '100%' });
  });
});
