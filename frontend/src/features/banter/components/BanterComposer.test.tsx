import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { BanterComposer } from './BanterComposer';

function renderComposer() {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<BanterComposer />, { queryClient });
}

describe('BanterComposer', () => {
  it('fills the textarea from a prompt chip and then hides the chips', async () => {
    const { user } = renderComposer();

    await user.click(screen.getByRole('button', { name: 'Hot take:' }));

    expect(screen.getByLabelText("What's on your mind?")).toHaveValue('Hot take: ');
    expect(screen.queryByRole('button', { name: 'Hot take:' })).not.toBeInTheDocument();
  });

  it('keeps Post disabled until there is text, and shows the characters left', async () => {
    const { user } = renderComposer();
    const post = screen.getByRole('button', { name: 'Post' });
    expect(post).toBeDisabled();

    await user.type(screen.getByLabelText("What's on your mind?"), 'Hello people');

    expect(post).toBeEnabled();
    expect(screen.getByText('268')).toBeInTheDocument();
  });

  it('posts the trimmed text and clears the box', async () => {
    let body: unknown;
    server.use(
      http.post('*/api/banter', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          {
            id: 'new-id',
            authorId: authUser.id,
            authorUsername: authUser.username,
            content: 'Hello people',
            createdAt: new Date().toISOString(),
            likeCount: 0,
            replyCount: 0,
            likedByMe: false,
            mine: true,
          },
          { status: 201 },
        );
      }),
    );
    const { user } = renderComposer();

    await user.type(screen.getByLabelText("What's on your mind?"), '  Hello people  ');
    await user.click(screen.getByRole('button', { name: 'Post' }));

    await waitFor(() => expect(screen.getByLabelText("What's on your mind?")).toHaveValue(''));
    expect(body).toEqual({ content: 'Hello people' });
  });

  it('posts with Ctrl+Enter', async () => {
    let posted = false;
    server.use(
      http.post('*/api/banter', () => {
        posted = true;
        return HttpResponse.json(
          {
            id: 'x',
            authorId: authUser.id,
            authorUsername: authUser.username,
            content: 'hi',
            createdAt: new Date().toISOString(),
            likeCount: 0,
            replyCount: 0,
            likedByMe: false,
            mine: true,
          },
          { status: 201 },
        );
      }),
    );
    const { user } = renderComposer();

    await user.type(screen.getByLabelText("What's on your mind?"), 'hi{Control>}{Enter}{/Control}');

    await waitFor(() => expect(posted).toBe(true));
  });

  it('shows the server error and keeps the text when posting fails', async () => {
    server.use(
      http.post('*/api/banter', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Content must not exceed 280 characters',
            path: '/api/banter',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderComposer();

    await user.type(screen.getByLabelText("What's on your mind?"), 'Something');
    await user.click(screen.getByRole('button', { name: 'Post' }));

    expect(await screen.findByText('Content must not exceed 280 characters')).toBeInTheDocument();
    expect(screen.getByLabelText("What's on your mind?")).toHaveValue('Something');
  });

  it('shows the Safety Guardian notice when the banter is refused', async () => {
    server.use(
      http.post('*/api/banter', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 422,
            error: 'UNPROCESSABLE_ENTITY',
            message: 'That reads like a threat. Please rephrase.',
            path: '/x',
            fieldErrors: {},
          },
          { status: 422 },
        ),
      ),
    );
    const { user } = renderComposer();

    await user.type(screen.getByLabelText("What's on your mind?"), 'something nasty');
    await user.click(screen.getByRole('button', { name: 'Post' }));

    expect(await screen.findByText('Safety Guardian paused this')).toBeInTheDocument();
    expect(screen.getByText('That reads like a threat. Please rephrase.')).toBeInTheDocument();
    expect(screen.getByLabelText("What's on your mind?")).toHaveValue('something nasty');
  });
});
