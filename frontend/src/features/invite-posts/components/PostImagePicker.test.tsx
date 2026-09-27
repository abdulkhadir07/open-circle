import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PostImagePicker } from './PostImagePicker';

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('PostImagePicker', () => {
  it('shows the count and no photos when empty', () => {
    render(<PostImagePicker value={[]} onChange={vi.fn<(files: File[]) => void>()} />);

    expect(screen.getByText('0/4 photos')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add a photo' })).toBeInTheDocument();
  });

  it('adds a valid photo and calls onChange with it appended', async () => {
    const onChange = vi.fn<(files: File[]) => void>();
    const user = userEvent.setup();
    const { container } = render(<PostImagePicker value={[]} onChange={onChange} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    const file = makeFile();
    await user.upload(fileInput, file);

    expect(onChange).toHaveBeenCalledWith([file]);
  });

  it('rejects an oversized image without calling onChange', async () => {
    const onChange = vi.fn<(files: File[]) => void>();
    const user = userEvent.setup();
    const { container } = render(<PostImagePicker value={[]} onChange={onChange} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(fileInput, makeFile({ sizeBytes: 6 * 1024 * 1024 }));

    expect(await screen.findByText(/exceeds the maximum/)).toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('rejects an unsupported file type client-side', async () => {
    const onChange = vi.fn<(files: File[]) => void>();
    const { container } = render(<PostImagePicker value={[]} onChange={onChange} />);
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    fireEvent.change(fileInput, {
      target: { files: [makeFile({ name: 'clip.mp4', type: 'video/mp4' })] },
    });

    expect(await screen.findByText(/not supported/)).toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('removes a staged photo when its remove button is clicked', async () => {
    const onChange = vi.fn<(files: File[]) => void>();
    const user = userEvent.setup();
    const file = makeFile();
    render(<PostImagePicker value={[file]} onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: 'Remove photo 1' }));

    expect(onChange).toHaveBeenCalledWith([]);
  });

  it('hides the add button once 4 photos are staged', () => {
    const files = Array.from({ length: 4 }, (_, index) => makeFile({ name: `photo-${index}.jpg` }));
    render(<PostImagePicker value={files} onChange={vi.fn<(files: File[]) => void>()} />);

    expect(screen.getByText('4/4 photos')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add a photo' })).not.toBeInTheDocument();
  });
});
