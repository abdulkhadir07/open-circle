import { describe, expect, it } from 'vitest';
import { formatTimeRemaining } from './formatTimeRemaining';

describe('formatTimeRemaining', () => {
  it('shows hours when at least one full hour remains', () => {
    expect(formatTimeRemaining(23 * 60 * 60 * 1000)).toBe('23h left');
  });

  it('shows minutes once under an hour remains', () => {
    expect(formatTimeRemaining(45 * 60 * 1000)).toBe('45m left');
  });

  it('shows a fallback message once under a minute remains', () => {
    expect(formatTimeRemaining(30 * 1000)).toBe('Less than a minute left');
  });

  it('treats zero or negative remaining time as expired', () => {
    expect(formatTimeRemaining(0)).toBe('Expired');
    expect(formatTimeRemaining(-1000)).toBe('Expired');
  });
});
