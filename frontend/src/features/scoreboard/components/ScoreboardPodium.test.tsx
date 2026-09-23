import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { scoreboard } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { ScoreboardPodium } from './ScoreboardPodium';

describe('ScoreboardPodium', () => {
  it('renders nothing when there are no entries', () => {
    const { container } = renderWithProviders(
      <ScoreboardPodium entries={[]} currentUserId={undefined} />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders every entry it is given, up to three, in Circle Points', () => {
    renderWithProviders(
      <ScoreboardPodium entries={scoreboard.entries} currentUserId={undefined} />,
    );

    scoreboard.entries.forEach((entry) => {
      expect(screen.getByText(entry.username)).toBeInTheDocument();
      expect(screen.getByText(`${entry.annualScore} Circle Points`)).toBeInTheDocument();
    });
  });

  it('renders a single entry fine when there is only one', () => {
    const [first] = scoreboard.entries;
    renderWithProviders(<ScoreboardPodium entries={[first!]} currentUserId={undefined} />);

    expect(screen.getByText(first!.username)).toBeInTheDocument();
  });

  it('marks the current user', () => {
    const [first] = scoreboard.entries;
    renderWithProviders(<ScoreboardPodium entries={[first!]} currentUserId={first!.userId} />);

    expect(screen.getByText('(you)')).toBeInTheDocument();
  });

  it('crowns only the first slot, and numbers the other slots by position rather than raw rank', () => {
    // Both entries share rank 2 in the underlying data (a tie) — the podium
    // should still show clean sequential slot numbers (1st slot gets the
    // crown, the 2nd slot is labeled "2"), not the raw tied rank value.
    const tiedRunnerUp = { ...scoreboard.entries[1]!, userId: 'tied-runner-up', rank: 2 };
    const tiedThird = { ...scoreboard.entries[1]!, userId: 'tied-third', rank: 2 };
    const { container } = renderWithProviders(
      <ScoreboardPodium
        entries={[scoreboard.entries[0]!, tiedRunnerUp, tiedThird]}
        currentUserId={undefined}
      />,
    );

    expect(container.querySelectorAll('svg.lucide-crown')).toHaveLength(1);
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });
});
