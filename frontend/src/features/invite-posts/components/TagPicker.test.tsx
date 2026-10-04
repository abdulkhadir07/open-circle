import { screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { MAX_TAGS } from '../lib/tags';
import { TagPicker } from './TagPicker';

function Harness({ initial = [] }: { initial?: string[] }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <TagPicker id="tags" value={value} onChange={setValue} />
      <output data-testid="value">{value.join('|')}</output>
    </>
  );
}

describe('TagPicker', () => {
  it('toggles a suggested topic on and off', async () => {
    const { user } = renderWithProviders(<Harness />);

    await user.click(screen.getByRole('button', { name: '#walk' }));
    expect(screen.getByTestId('value')).toHaveTextContent('walk');
    expect(screen.getByRole('button', { name: '#walk', pressed: true })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '#walk' }));
    expect(screen.getByTestId('value')).toBeEmptyDOMElement();
  });

  it('adds a custom topic normalized, and lets it be removed', async () => {
    const { user } = renderWithProviders(<Harness />);

    await user.type(screen.getByRole('textbox'), '#Rock Climbing{Enter}');
    expect(screen.getByTestId('value')).toHaveTextContent('rock-climbing');
    expect(screen.getByText('#rock-climbing')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Remove rock-climbing' }));
    expect(screen.getByTestId('value')).toBeEmptyDOMElement();
  });

  it('ignores duplicates and blank input', async () => {
    const { user } = renderWithProviders(<Harness initial={['walk']} />);

    await user.type(screen.getByRole('textbox'), 'Walk{Enter}');
    await user.type(screen.getByRole('textbox'), '   {Enter}');

    expect(screen.getByTestId('value')).toHaveTextContent(/^walk$/);
  });

  it('stops accepting topics at the limit', async () => {
    const full = ['a', 'b', 'c', 'd', 'e'].slice(0, MAX_TAGS);
    renderWithProviders(<Harness initial={full} />);

    expect(screen.getByRole('textbox')).toBeDisabled();
    expect(screen.getByRole('button', { name: '#walk' })).toBeDisabled();
  });
});
