import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { invitePost, invitePostImage, invitePostWithImages } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
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

  it('only offers edit and delete actions on the owner card', async () => {
    const { user, unmount } = renderWithProviders(<InvitePostCard post={invitePost} />);
    expect(screen.queryByRole('button', { name: 'Invite actions' })).not.toBeInTheDocument();
    unmount();

    renderWithProviders(<InvitePostCard post={invitePost} isOwnPost />);
    await user.click(screen.getByRole('button', { name: 'Invite actions' }));

    expect(screen.getByRole('menuitem', { name: 'Edit invite' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Delete invite' })).toBeInTheDocument();
  });

  it('submits edited invite content', async () => {
    let updateBody: unknown;
    server.use(
      http.patch('*/api/invite-posts/:postId', async ({ request }) => {
        updateBody = await request.json();
        return HttpResponse.json({ ...invitePost, ...(updateBody as object) });
      }),
    );
    const { user } = renderWithProviders(<InvitePostCard post={invitePost} isOwnPost />);

    await user.click(screen.getByRole('button', { name: 'Invite actions' }));
    await user.click(screen.getByRole('menuitem', { name: 'Edit invite' }));
    const content = screen.getByLabelText("What's the invite?");
    await user.clear(content);
    await user.type(content, 'Coffee moved to six this evening');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(updateBody).toEqual({
        content: 'Coffee moved to six this evening',
        inviteType: 'SINGLE',
        totalCapacity: 1,
        locationScope: 'CITY',
        tags: [],
      }),
    );
  });

  it('confirms before deleting an owned invite', async () => {
    let deletedPostId: string | undefined;
    server.use(
      http.delete('*/api/invite-posts/:postId', ({ params }) => {
        deletedPostId = String(params.postId);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderWithProviders(<InvitePostCard post={invitePost} isOwnPost />);

    await user.click(screen.getByRole('button', { name: 'Invite actions' }));
    await user.click(screen.getByRole('menuitem', { name: 'Delete invite' }));
    expect(screen.getByText('Delete this invite?')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Delete invite' }));

    await waitFor(() => expect(deletedPostId).toBe(invitePost.id));
    expect(screen.queryByText('Delete this invite?')).not.toBeInTheDocument();
  });
});
