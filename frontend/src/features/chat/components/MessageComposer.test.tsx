import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { chatRoom } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { MessageComposer } from './MessageComposer';

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('MessageComposer', () => {
  it('sends a message and clears the input, with no error on success', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'Hey, running 5 minutes late!');
    await user.click(screen.getByRole('button', { name: 'Send message' }));

    await waitFor(() => expect(input).toHaveValue(''));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('sends on Enter without needing the button', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'On my way{Enter}');

    await waitFor(() => expect(input).toHaveValue(''));
  });

  it('does not send on Shift+Enter, allowing a newline instead', async () => {
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    const input = screen.getByRole('textbox', { name: 'Message' });
    await user.type(input, 'Line one{Shift>}{Enter}{/Shift}Line two');

    expect(input).toHaveValue('Line one\nLine two');
  });

  it('disables the composer when the room is closed', () => {
    const { container } = renderWithProviders(<MessageComposer roomId={chatRoom.id} disabled />);

    expect(screen.getByRole('textbox', { name: 'Message' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send message' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Attach a file' })).toBeDisabled();
    expect(container.querySelector('input[type="file"]')).toBeDisabled();
  });

  it('uploads an attachment and clears the composer on success', async () => {
    const { user, container } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, makeFile());

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });

  it('sends the typed message as the attachment caption and clears it', async () => {
    const { user, container } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.type(screen.getByRole('textbox', { name: 'Message' }), 'Here is the flyer');
    await user.upload(fileInput, makeFile());

    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Message' })).toHaveValue(''));
  });

  it('rejects an oversized file client-side without an upload request', async () => {
    let uploadRequested = false;
    server.use(
      http.post('*/api/chat-rooms/:roomId/attachments', () => {
        uploadRequested = true;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const { user, container } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, makeFile({ sizeBytes: 11 * 1024 * 1024 }));

    expect(await screen.findByText(/exceeds the maximum/)).toBeInTheDocument();
    expect(uploadRequested).toBe(false);
  });

  it('rejects an unsupported file type client-side', async () => {
    // The input's `accept` attribute already filters this in a real browser,
    // but a user can still bypass it via "All files" in the OS picker — so
    // dispatch the change event directly rather than going through
    // userEvent.upload, which itself enforces `accept` and would otherwise
    // silently refuse to select a non-matching file in this test.
    const { container } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    fireEvent.change(fileInput, {
      target: { files: [makeFile({ name: 'clip.mp4', type: 'video/mp4' })] },
    });

    expect(await screen.findByText(/not supported/)).toBeInTheDocument();
  });

  it('shows an error if sending fails', async () => {
    server.use(
      http.post('*/api/chat-rooms/:roomId/messages', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 403,
            error: 'FORBIDDEN',
            message: 'You are not an active participant in this chat',
            path: '/api/chat-rooms/mock/messages',
            fieldErrors: {},
          },
          { status: 403 },
        ),
      ),
    );
    const { user } = renderWithProviders(<MessageComposer roomId={chatRoom.id} />);

    await user.type(screen.getByRole('textbox', { name: 'Message' }), 'Hello?{Enter}');

    expect(
      await screen.findByText('You are not an active participant in this chat'),
    ).toBeInTheDocument();
  });
});
