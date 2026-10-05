import { describe, expect, it } from 'vitest';
import { MAX_TAG_LENGTH, normalizeTag, parseTagList } from './tags';

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

describe('parseTagList', () => {
  it('normalises, drops blanks and duplicates, and keeps at most five', () => {
    expect(parseTagList(['#Walk', ' study ', '', 'walk', 'Board Games'])).toEqual([
      'walk',
      'study',
      'board-games',
    ]);
    expect(parseTagList(['a', 'b', 'c', 'd', 'e', 'f'])).toHaveLength(5);
  });
});
