import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Avatar } from './avatar';

describe('Avatar', () => {
  it('renders the first letter of the name when no photo is set', () => {
    render(<Avatar name="Maya" profileImage={null} />);

    expect(screen.getByText('M')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('renders the letter fallback when profileImage is undefined', () => {
    render(<Avatar name="Jordan" />);

    expect(screen.getByText('J')).toBeInTheDocument();
  });

  it('renders the photo when one is set', () => {
    const { container } = render(
      <Avatar name="Maya" profileImage={{ url: 'https://storage.example.com/maya.jpg' }} />,
    );

    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    expect(img?.src).toBe('https://storage.example.com/maya.jpg');
    expect(screen.queryByText('M')).not.toBeInTheDocument();
  });

  it('applies the passed className to whichever variant renders', () => {
    const { container, rerender } = render(
      <Avatar name="Maya" profileImage={null} className="size-9" />,
    );
    expect(screen.getByText('M')).toHaveClass('size-9');

    rerender(
      <Avatar
        name="Maya"
        profileImage={{ url: 'https://storage.example.com/maya.jpg' }}
        className="size-9"
      />,
    );
    expect(container.querySelector('img')).toHaveClass('size-9');
  });
});
