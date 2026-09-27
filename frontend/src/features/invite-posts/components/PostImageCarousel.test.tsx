import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { invitePostImage } from '@/test/mocks/fixtures';
import { PostImageCarousel } from './PostImageCarousel';

describe('PostImageCarousel', () => {
  it('renders nothing when there are no images', () => {
    const { container } = render(<PostImageCarousel images={[]} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('renders a single image with no navigation controls', () => {
    render(<PostImageCarousel images={[invitePostImage]} />);

    expect(screen.getByRole('img')).toHaveAttribute('src', invitePostImage.url);
    expect(screen.queryByRole('button', { name: 'Next photo' })).not.toBeInTheDocument();
  });

  it('shows navigation controls and dots for multiple images, starting on the first', () => {
    const second = {
      ...invitePostImage,
      id: 'image-2',
      displayOrder: 2,
      url: 'https://example.com/2.jpg',
    };
    render(<PostImageCarousel images={[invitePostImage, second]} />);

    expect(screen.getByRole('img')).toHaveAttribute('src', invitePostImage.url);
    expect(screen.getByRole('button', { name: 'Go to photo 1' })).toHaveAttribute(
      'aria-current',
      'true',
    );
  });

  it('advances to the next image when "Next photo" is clicked', async () => {
    const second = {
      ...invitePostImage,
      id: 'image-2',
      displayOrder: 2,
      url: 'https://example.com/2.jpg',
    };
    const user = userEvent.setup();
    render(<PostImageCarousel images={[invitePostImage, second]} />);

    await user.click(screen.getByRole('button', { name: 'Next photo' }));

    expect(screen.getByRole('img')).toHaveAttribute('src', second.url);
  });

  it('wraps from the last image back to the first', async () => {
    const second = {
      ...invitePostImage,
      id: 'image-2',
      displayOrder: 2,
      url: 'https://example.com/2.jpg',
    };
    const user = userEvent.setup();
    render(<PostImageCarousel images={[invitePostImage, second]} />);

    await user.click(screen.getByRole('button', { name: 'Previous photo' }));

    expect(screen.getByRole('img')).toHaveAttribute('src', second.url);
  });

  it('jumps directly to an image when its dot is clicked', async () => {
    const second = {
      ...invitePostImage,
      id: 'image-2',
      displayOrder: 2,
      url: 'https://example.com/2.jpg',
    };
    const user = userEvent.setup();
    render(<PostImageCarousel images={[invitePostImage, second]} />);

    await user.click(screen.getByRole('button', { name: 'Go to photo 2' }));

    expect(screen.getByRole('img')).toHaveAttribute('src', second.url);
  });
});
