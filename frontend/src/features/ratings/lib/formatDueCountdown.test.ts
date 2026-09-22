import { describe, expect, it } from 'vitest';
import { formatDueCountdown } from './formatDueCountdown';

describe('formatDueCountdown', () => {
  it('shows hours when at least one full hour remains', () => {
    expect(formatDueCountdown(23 * 60 * 60 * 1000)).toBe('23h left to rate');
  });

  it('shows minutes once under an hour remains', () => {
    expect(formatDueCountdown(45 * 60 * 1000)).toBe('45m left to rate');
  });

  it('shows a fallback message once under a minute remains', () => {
    expect(formatDueCountdown(30 * 1000)).toBe('Less than a minute left to rate');
  });

  it('treats zero or negative remaining time as expired', () => {
    expect(formatDueCountdown(0)).toBe('Expired');
    expect(formatDueCountdown(-1000)).toBe('Expired');
  });
});
