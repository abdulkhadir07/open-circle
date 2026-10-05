import { describe, expect, it } from 'vitest';
import { formatShortAgo } from './formatShortAgo';

const NOW = Date.parse('2026-10-04T12:00:00Z');
const ago = (ms: number) => new Date(NOW - ms).toISOString();

describe('formatShortAgo', () => {
  it('reads "just now" under a minute', () => {
    expect(formatShortAgo(ago(30_000), NOW)).toBe('just now');
  });

  it('uses minutes, hours, then days with no "ago"', () => {
    expect(formatShortAgo(ago(5 * 60_000), NOW)).toBe('5m');
    expect(formatShortAgo(ago(3 * 3_600_000), NOW)).toBe('3h');
    expect(formatShortAgo(ago(2 * 86_400_000), NOW)).toBe('2d');
    expect(formatShortAgo(ago(20 * 86_400_000), NOW)).toBe('20d');
  });
});
