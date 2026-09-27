import { describe, expect, it } from 'vitest';
import { formatFileSize } from './formatFileSize';

describe('formatFileSize', () => {
  it('formats sizes under 1 KB in bytes', () => {
    expect(formatFileSize(512)).toBe('512 B');
  });

  it('formats sizes under 1 MB in whole kilobytes', () => {
    expect(formatFileSize(245 * 1024)).toBe('245 KB');
  });

  it('formats sizes of 1 MB and above in megabytes with one decimal', () => {
    expect(formatFileSize(2.5 * 1024 * 1024)).toBe('2.5 MB');
  });
});
