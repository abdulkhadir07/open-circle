import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { invitePost, invitePostImage, invitePostWithImages } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { InvitePostCard } from './InvitePostCard';

describe('InvitePostCard', () => {
  it('shows the poster and content for a single invite, with no location line', () => {
    renderWithProviders(<InvitePostCard post={invitePost} />);

    expect(screen.getByText(invitePost.posterUsername)).toBeInTheDocument();
    expect(screen.getByText(invitePost.content)).toBeInTheDocument();
    expect(screen.queryByText(/San Francisco/)).not.toBeInTheDocument();
    expect(screen.queryByText('City')).not.toBeInTheDocument();
    expect(screen.getByText('1 spot left')).toBeInTheDocument();
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

    expect(screen.getByText('3 spots left')).toBeInTheDocument();
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

    expect(screen.getByText('24 spots left')).toBeInTheDocument();
  });

  it('says Full once a single invite has been accepted', () => {
    renderWithProviders(
      <InvitePostCard post={{ ...invitePost, acceptedCount: 1, invitesLeft: 0 }} />,
    );

    expect(screen.getByText('Full')).toBeInTheDocument();
    expect(screen.queryByText('1 spot left')).not.toBeInTheDocument();
  });

  it('shows topic chips as plain labels when no handler is given', () => {
    renderWithProviders(<InvitePostCard post={{ ...invitePost, tags: ['walk', 'code'] }} />);

    expect(screen.getByText('#walk')).toBeInTheDocument();
    expect(screen.getByText('#code')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '#walk' })).not.toBeInTheDocument();
  });

  it('makes topic chips clickable when a handler is given', async () => {
    const onTagClick = vi.fn<(tag: string) => void>();
    const { user } = renderWithProviders(
      <InvitePostCard post={{ ...invitePost, tags: ['walk'] }} onTagClick={onTagClick} />,
    );

    await user.click(screen.getByRole('button', { name: '#walk' }));

    expect(onTagClick).toHaveBeenCalledWith('walk');
  });

  it('shows no topic row when the post has none', () => {
    renderWithProviders(<InvitePostCard post={invitePost} />);

    expect(screen.queryByText(/^#/)).not.toBeInTheDocument();
  });

  it("links the poster's avatar and name to their profile", () => {
    renderWithProviders(<InvitePostCard post={invitePost} />);

    expect(screen.getByRole('link', { name: invitePost.posterUsername })).toHaveAttribute(
      'href',
      `/profile/${invitePost.posterId}`,
    );
  });

  it('shows why an invite suits you, but never on your own invite', () => {
    const { unmount } = renderWithProviders(
      <InvitePostCard post={invitePost} reason="Matches your interest in coffee" />,
    );
    expect(screen.getByText('Matches your interest in coffee')).toBeInTheDocument();
    unmount();

    renderWithProviders(
      <InvitePostCard post={invitePost} isOwnPost reason="Matches your interest in coffee" />,
    );
    expect(screen.queryByText('Matches your interest in coffee')).not.toBeInTheDocument();
  });
});
