import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, invitePost } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { CreatePostDialog } from './CreatePostDialog';

function makeImageFile(name = 'photo.jpg') {
  return new File([new Uint8Array(1024)], name, { type: 'image/jpeg' });
}

function renderDialog(userOverrides: Partial<typeof authUser> = {}) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, { ...authUser, ...userOverrides });
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(<CreatePostDialog />, { queryClient });
}

describe('CreatePostDialog', () => {
  it('creates a single invite post and closes the dialog', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('requires a group size to be chosen for a group invite', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(screen.getByLabelText("What's the invite?"), 'Beach day, who is in?');
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    expect(
      await screen.findByText('Group invites need a capacity of at least 2'),
    ).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('offers a dropdown of group sizes starting at 2', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));

    expect(screen.getByRole('option', { name: '2 people' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '1 people' })).not.toBeInTheDocument();
  });

  it('lets a group size beyond the dropdown be typed in directly', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(screen.getByLabelText("What's the invite?"), 'Big community cleanup day');
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));
    await user.click(screen.getByRole('option', { name: 'Custom number…' }));

    const customInput = screen.getByLabelText('How many people can join?');
    await user.type(customInput, '25');
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('lets a custom group size be abandoned back to the dropdown', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('radio', { name: 'Group' }));
    await user.click(screen.getByRole('combobox', { name: 'How many people can join?' }));
    await user.click(screen.getByRole('option', { name: 'Custom number…' }));
    await user.click(screen.getByRole('button', { name: 'Choose from list' }));

    expect(screen.getByRole('combobox', { name: 'How many people can join?' })).toBeInTheDocument();
  });

  it('only offers State as a scope when the poster has a verified state/region', async () => {
    const { user } = renderDialog({ verifiedStateRegion: undefined });

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.click(screen.getByRole('combobox', { name: 'Who can see it' }));

    expect(screen.getByRole('option', { name: 'City' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'State' })).not.toBeInTheDocument();
  });

  it('uploads a staged photo after creating the post and still closes the dialog', async () => {
    let uploadRequested = false;
    server.use(
      http.post('*/api/invite-posts/:postId/images', () => {
        uploadRequested = true;
        return HttpResponse.json(
          {
            id: 'image-1',
            url: 'https://example.com/photo.jpg',
            urlExpiresAt: new Date().toISOString(),
            originalFilename: 'photo.jpg',
            contentType: 'image/jpeg',
            fileSizeBytes: 1024,
            displayOrder: 1,
            createdAt: new Date().toISOString(),
          },
          { status: 201 },
        );
      }),
    );
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(fileInput, makeImageFile());
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(uploadRequested).toBe(true);
  });

  it('shows a recovery view when the post is created but a photo upload fails', async () => {
    server.use(
      http.post('*/api/invite-posts', () => HttpResponse.json(invitePost, { status: 201 })),
      http.post('*/api/invite-posts/:postId/images', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'File type is not supported',
            path: '/api/invite-posts/mock/images',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(fileInput, makeImageFile());
    await user.click(screen.getByRole('button', { name: 'Post invite' }));

    expect(await screen.findByText('Your invite was posted')).toBeInTheDocument();
    expect(screen.getByText('File type is not supported')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('closes the dialog after a successful retry clears the last failed upload', async () => {
    let shouldFail = true;
    server.use(
      http.post('*/api/invite-posts', () => HttpResponse.json(invitePost, { status: 201 })),
      http.post('*/api/invite-posts/:postId/images', () => {
        if (shouldFail) {
          shouldFail = false;
          return HttpResponse.json(
            {
              timestamp: new Date().toISOString(),
              status: 400,
              error: 'BAD_REQUEST',
              message: 'File type is not supported',
              path: '/api/invite-posts/mock/images',
              fieldErrors: {},
            },
            { status: 400 },
          );
        }
        return HttpResponse.json(
          {
            id: 'image-1',
            url: 'https://example.com/photo.jpg',
            urlExpiresAt: new Date().toISOString(),
            originalFilename: 'photo.jpg',
            contentType: 'image/jpeg',
            fileSizeBytes: 1024,
            displayOrder: 1,
            createdAt: new Date().toISOString(),
          },
          { status: 201 },
        );
      }),
    );
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(fileInput, makeImageFile());
    await user.click(screen.getByRole('button', { name: 'Post invite' }));
    await screen.findByText('Your invite was posted');

    await user.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('lets the user finish without a failed photo via Done', async () => {
    server.use(
      http.post('*/api/invite-posts', () => HttpResponse.json(invitePost, { status: 201 })),
      http.post('*/api/invite-posts/:postId/images', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'File type is not supported',
            path: '/api/invite-posts/mock/images',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'New post' }));
    await user.type(
      screen.getByLabelText("What's the invite?"),
      'Anyone up for coffee this afternoon?',
    );
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(fileInput, makeImageFile());
    await user.click(screen.getByRole('button', { name: 'Post invite' }));
    await screen.findByText('Your invite was posted');

    await user.click(screen.getByRole('button', { name: 'Done' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
