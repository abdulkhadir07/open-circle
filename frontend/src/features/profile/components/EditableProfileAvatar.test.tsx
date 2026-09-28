import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useAuthStore } from '@/stores/authStore';
import { authUser, profileImage } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { EditableProfileAvatar } from './EditableProfileAvatar';

function renderAvatar(hasPhoto = false) {
  const queryClient = createTestQueryClient();
  queryClient.setQueryData(authQueryKeys.currentUser, authUser);
  useAuthStore.getState().setAuthenticated('access-token');
  return renderWithProviders(
    <EditableProfileAvatar name="Maya Chen" profileImage={hasPhoto ? profileImage : null} />,
    { queryClient },
  );
}

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('EditableProfileAvatar', () => {
  it('offers to upload with no remove option when no photo is set', async () => {
    const { user } = renderAvatar(false);

    await user.click(screen.getByRole('button', { name: 'Edit profile photo' }));

    expect(screen.getByRole('menuitem', { name: 'Upload photo' })).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: 'Remove photo' })).not.toBeInTheDocument();
  });

  it('offers to change or remove an existing photo', async () => {
    const { user } = renderAvatar(true);

    await user.click(screen.getByRole('button', { name: 'Edit profile photo' }));

    expect(screen.getByRole('menuitem', { name: 'Change photo' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Remove photo' })).toBeInTheDocument();
  });

  it('removes the photo when Remove photo is selected', async () => {
    const { user } = renderAvatar(true);

    await user.click(screen.getByRole('button', { name: 'Edit profile photo' }));
    await user.click(screen.getByRole('menuitem', { name: 'Remove photo' }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  it('rejects an oversized file without calling the API', async () => {
    renderAvatar(false);

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [makeFile({ sizeBytes: 6 * 1024 * 1024 })] } });

    expect(await screen.findByText(/exceeds the maximum/)).toBeInTheDocument();
  });

  it('rejects an unsupported file type without calling the API', async () => {
    renderAvatar(false);

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, {
      target: { files: [makeFile({ name: 'clip.mp4', type: 'video/mp4' })] },
    });

    expect(await screen.findByText(/not supported/)).toBeInTheDocument();
  });

  it('uploads a valid photo with no validation or backend error', async () => {
    renderAvatar(false);

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [makeFile()] } });

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  it('shows a backend error when upload fails', async () => {
    server.use(
      http.put('*/api/users/me/profile-image', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Unable to store uploaded file',
            path: '/api/users/me/profile-image',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    renderAvatar(false);

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [makeFile()] } });

    expect(await screen.findByText('Unable to store uploaded file')).toBeInTheDocument();
  });
});
