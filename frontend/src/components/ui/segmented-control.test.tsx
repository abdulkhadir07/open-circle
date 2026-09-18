import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { SegmentedControl } from './segmented-control';

function Example() {
  const [value, setValue] = useState<'local' | 'global'>('local');
  return (
    <SegmentedControl
      aria-label="Feed"
      value={value}
      onChange={setValue}
      options={[
        { value: 'local', label: 'Local' },
        { value: 'global', label: 'Global' },
      ]}
    />
  );
}

describe('SegmentedControl', () => {
  it('marks only the selected option as checked and switches on click', async () => {
    const user = userEvent.setup();
    render(<Example />);

    expect(screen.getByRole('radio', { name: 'Local' })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: 'Global' })).toHaveAttribute('aria-checked', 'false');

    await user.click(screen.getByRole('radio', { name: 'Global' }));

    expect(screen.getByRole('radio', { name: 'Local' })).toHaveAttribute('aria-checked', 'false');
    expect(screen.getByRole('radio', { name: 'Global' })).toHaveAttribute('aria-checked', 'true');
  });
});
