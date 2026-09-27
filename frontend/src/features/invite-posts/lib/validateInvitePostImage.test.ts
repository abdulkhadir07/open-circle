import { describe, expect, it } from 'vitest';
import { validateInvitePostImage } from './validateInvitePostImage';

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('validateInvitePostImage', () => {
  it('accepts an allowed type under the size limit', () => {
    expect(validateInvitePostImage(makeFile())).toBeNull();
  });

  it('rejects a file over 5 MB', () => {
    const message = validateInvitePostImage(makeFile({ sizeBytes: 6 * 1024 * 1024 }));
    expect(message).toMatch(/exceeds the maximum/);
  });

  it('rejects an unsupported content type', () => {
    const message = validateInvitePostImage(makeFile({ name: 'doc.pdf', type: 'application/pdf' }));
    expect(message).toMatch(/not supported/);
  });
});
