import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { invitePost, invitePostImage, invitePostWithImages } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { InvitePostCard } from './InvitePostCard';

describe('InvitePostCard', () => {
  it('shows the poster, content, and scope for a single invite', () => {
    renderWithProviders(<InvitePostCard post={invitePost} />);

    expect(screen.getByText(invitePost.posterUsername)).toBeInTheDocument();
    expect(screen.getByText(invitePost.content)).toBeInTheDocument();
    expect(screen.getByText(`${invitePost.city}, ${invitePost.country}`)).toBeInTheDocument();
    expect(screen.getByText('City')).toBeInTheDocument();
    expect(screen.queryByText(/invites? left|\d+ left/)).not.toBeInTheDocument();
  });

  it('shows no image carousel when the post has no images', () => {
    renderWithProviders(<InvitePostCard post={invitePost} />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows the image carousel when the post has images', () => {
    renderWithProviders(<InvitePostCard post={invitePostWithImages} />);

    expect(screen.getByRole('img')).toHaveAttribute('src', invitePostImage.url);
    expect(screen.getByRole('button', { name: 'Next photo' })).toBeInTheDocument();
  });

  it('shows capacity dots and the remaining count for a small group invite', () => {
    renderWithProviders(
      <InvitePostCard
        post={{
          ...invitePost,
          inviteType: 'GROUP',
          totalCapacity: 4,
          acceptedCount: 1,
          invitesLeft: 3,
        }}
      />,
    );

    expect(screen.getByText('3 left')).toBeInTheDocument();
  });

  it('falls back to plain text once capacity is too large to show as dots', () => {
    renderWithProviders(
      <InvitePostCard
        post={{
          ...invitePost,
          inviteType: 'GROUP',
          totalCapacity: 25,
          acceptedCount: 1,
          invitesLeft: 24,
        }}
      />,
    );

    expect(screen.getByText('24 invites left')).toBeInTheDocument();
  });
});
