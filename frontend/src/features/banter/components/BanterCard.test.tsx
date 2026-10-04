import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { banter, banterReply } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { BanterCard } from './BanterCard';

describe('BanterCard', () => {
  it('shows the author, text, and counts, with the author linking to their profile', () => {
    renderWithProviders(<BanterCard banter={banter} />);

    expect(screen.getByText(banter.content)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: banter.authorUsername })).toHaveAttribute(
      'href',
      `/profile/${banter.authorId}`,
    );
    expect(screen.getByRole('button', { name: 'Like' })).toHaveTextContent('2');
    expect(screen.getByRole('button', { name: '1 reply' })).toBeInTheDocument();
  });

  it('only offers delete on your own banter', () => {
    const { unmount } = renderWithProviders(<BanterCard banter={banter} />);
    expect(screen.queryByRole('button', { name: 'Delete banter' })).not.toBeInTheDocument();
    unmount();

    renderWithProviders(<BanterCard banter={{ ...banter, mine: true }} />);
    expect(screen.getByRole('button', { name: 'Delete banter' })).toBeInTheDocument();
  });

  it('asks for confirmation before deleting, and deletes on confirm', async () => {
    let deletedId: string | undefined;
    server.use(
      http.delete('*/api/banter/:banterId', ({ params }) => {
        deletedId = params.banterId as string;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { user } = renderWithProviders(<BanterCard banter={{ ...banter, mine: true }} />);

    await user.click(screen.getByRole('button', { name: 'Delete banter' }));
    expect(await screen.findByText('Delete this banter?')).toBeInTheDocument();
    expect(deletedId).toBeUndefined();

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    await screen.findByRole('button', { name: 'Delete banter' });
    expect(deletedId).toBe(banter.id);
  });

  it('shows the like button as pressed when you already liked it', () => {
    renderWithProviders(<BanterCard banter={{ ...banter, likedByMe: true }} />);

    expect(screen.getByRole('button', { name: 'Unlike' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('opens the thread, lists replies, and posts a new reply', async () => {
    let replyBody: unknown;
    server.use(
      http.get('*/api/banter/:banterId/replies', () => HttpResponse.json([banterReply])),
      http.post('*/api/banter/:banterId/replies', async ({ request }) => {
        replyBody = await request.json();
        return HttpResponse.json(
          { ...banterReply, content: 'Agreed!', mine: true },
          { status: 201 },
        );
      }),
    );
    const { user } = renderWithProviders(<BanterCard banter={banter} />);

    await user.click(screen.getByRole('button', { name: '1 reply' }));
    expect(await screen.findByText(banterReply.content)).toBeInTheDocument();

    await user.type(screen.getByLabelText('Write a reply'), 'Agreed!');
    await user.click(screen.getByRole('button', { name: 'Send reply' }));

    await screen.findByLabelText('Write a reply');
    expect(replyBody).toEqual({ content: 'Agreed!' });
  });

  it('only offers delete on your own replies', async () => {
    server.use(
      http.get('*/api/banter/:banterId/replies', () =>
        HttpResponse.json([banterReply, { ...banterReply, id: 'mine-reply', mine: true }]),
      ),
    );
    const { user } = renderWithProviders(<BanterCard banter={banter} />);

    await user.click(screen.getByRole('button', { name: '1 reply' }));
    await screen.findAllByText(banterReply.content);

    expect(screen.getAllByRole('button', { name: 'Delete reply' })).toHaveLength(1);
  });
});
