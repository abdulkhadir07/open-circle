import { describe, expect, it } from 'vitest';
import { formatCampusName } from './campus';

describe('formatCampusName', () => {
  it('upper-cases short acronym-style domains', () => {
    expect(formatCampusName('sfsu.edu')).toBe('SFSU');
    expect(formatCampusName('mit.edu')).toBe('MIT');
    expect(formatCampusName('ucla.edu')).toBe('UCLA');
  });

  it('capitalises longer names', () => {
    expect(formatCampusName('stanford.edu')).toBe('Stanford');
    expect(formatCampusName('berkeley.edu')).toBe('Berkeley');
  });

  it('is case-insensitive and ignores surrounding space', () => {
    expect(formatCampusName(' SFSU.EDU ')).toBe('SFSU');
  });

  it('falls back to a generic phrase when there is no campus', () => {
    expect(formatCampusName(undefined)).toBe('your campus');
    expect(formatCampusName('')).toBe('your campus');
  });
});
