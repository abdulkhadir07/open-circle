import { describe, expect, it } from 'vitest';
import { validateProfileImage } from './validateProfileImage';

function makeFile({
  name = 'photo.jpg',
  type = 'image/jpeg',
  sizeBytes = 1024,
}: { name?: string; type?: string; sizeBytes?: number } = {}) {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('validateProfileImage', () => {
  it('accepts an allowed type under the size limit', () => {
    expect(validateProfileImage(makeFile())).toBeNull();
  });

  it('rejects a file over 5 MB', () => {
    const message = validateProfileImage(makeFile({ sizeBytes: 6 * 1024 * 1024 }));
    expect(message).toMatch(/exceeds the maximum/);
  });

  it('rejects an unsupported content type', () => {
    const message = validateProfileImage(makeFile({ name: 'doc.pdf', type: 'application/pdf' }));
    expect(message).toMatch(/not supported/);
  });
});
