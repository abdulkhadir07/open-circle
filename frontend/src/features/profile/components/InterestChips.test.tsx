import { screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { InterestChips } from './InterestChips';

function ControlledInterestChips({ initial = [] as string[] }) {
  const [value, setValue] = useState<string[]>(initial);
  return <InterestChips value={value} onChange={setValue} />;
}

describe('InterestChips', () => {
  it('adds an interest on Enter', async () => {
    const { user } = renderWithProviders(<ControlledInterestChips />);

    await user.type(screen.getByRole('textbox'), 'Hiking{Enter}');

    expect(screen.getByText('Hiking')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveValue('');
  });

  it('adds an interest on comma', async () => {
    const { user } = renderWithProviders(<ControlledInterestChips />);

    await user.type(screen.getByRole('textbox'), 'Coffee,');

    expect(screen.getByText('Coffee')).toBeInTheDocument();
  });

  it('commits the draft on blur', async () => {
    const { user } = renderWithProviders(<ControlledInterestChips />);

    await user.type(screen.getByRole('textbox'), 'Live music');
    await user.tab();

    expect(screen.getByText('Live music')).toBeInTheDocument();
  });

  it('removes an interest when its remove button is clicked', async () => {
    const { user } = renderWithProviders(<ControlledInterestChips initial={['Hiking']} />);

    await user.click(screen.getByRole('button', { name: 'Remove Hiking' }));

    expect(screen.queryByText('Hiking')).not.toBeInTheDocument();
  });

  it('removes the last interest with Backspace on an empty draft', async () => {
    const { user } = renderWithProviders(
      <ControlledInterestChips initial={['Hiking', 'Coffee']} />,
    );

    await user.click(screen.getByRole('textbox'));
    await user.keyboard('{Backspace}');

    expect(screen.queryByText('Coffee')).not.toBeInTheDocument();
    expect(screen.getByText('Hiking')).toBeInTheDocument();
  });

  it('does not add a duplicate interest, ignoring case', async () => {
    const { user } = renderWithProviders(<ControlledInterestChips initial={['Hiking']} />);

    await user.type(screen.getByRole('textbox'), 'hiking{Enter}');

    expect(screen.getAllByText(/hiking/i)).toHaveLength(1);
  });

  it('disables the input once 8 interests are reached', () => {
    renderWithProviders(
      <ControlledInterestChips initial={['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']} />,
    );

    expect(screen.getByRole('textbox')).toBeDisabled();
  });
});
