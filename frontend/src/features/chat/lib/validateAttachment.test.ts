import { describe, expect, it } from 'vitest';
import { validateAttachment } from './validateAttachment';

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('validateAttachment', () => {
  it('accepts an allowed type under the size limit', () => {
    expect(validateAttachment(makeFile())).toBeNull();
  });

  it('accepts a PDF', () => {
    expect(validateAttachment(makeFile({ name: 'doc.pdf', type: 'application/pdf' }))).toBeNull();
  });

  it('rejects a file over 10 MB', () => {
    const message = validateAttachment(makeFile({ sizeBytes: 11 * 1024 * 1024 }));
    expect(message).toMatch(/exceeds the maximum/);
  });

  it('rejects an unsupported content type', () => {
    const message = validateAttachment(makeFile({ name: 'clip.mp4', type: 'video/mp4' }));
    expect(message).toMatch(/not supported/);
  });
});
