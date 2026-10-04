import { describe, expect, it } from 'vitest';
import { MAX_TAG_LENGTH, normalizeTag } from './tags';

describe('normalizeTag', () => {
  it('lowercases and strips leading hashes', () => {
    expect(normalizeTag('#Walk')).toBe('walk');
    expect(normalizeTag('  ##Study ')).toBe('study');
  });

  it('joins words with hyphens', () => {
    expect(normalizeTag('Board  Games')).toBe('board-games');
  });

  it('drops characters the backend would reject', () => {
    expect(normalizeTag('c++ jam!')).toBe('c-jam');
  });

  it('returns an empty string when nothing usable is left', () => {
    expect(normalizeTag('  # ')).toBe('');
    expect(normalizeTag('---')).toBe('');
  });

  it('caps the length', () => {
    expect(normalizeTag('a'.repeat(50))).toHaveLength(MAX_TAG_LENGTH);
  });
});
