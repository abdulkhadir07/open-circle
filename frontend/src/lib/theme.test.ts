import { afterEach, describe, expect, it } from 'vitest';
import { applyTheme, getStoredTheme, THEME_STORAGE_KEY } from './theme';

afterEach(() => {
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
});

describe('theme', () => {
  it('defaults to system when nothing is stored', () => {
    expect(getStoredTheme()).toBe('system');
  });

  it('forces light or dark on the document and remembers the choice', () => {
    applyTheme('dark');
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(getStoredTheme()).toBe('dark');

    applyTheme('light');
    expect(document.documentElement).toHaveAttribute('data-theme', 'light');
    expect(getStoredTheme()).toBe('light');
  });

  it('clears the override and the stored value when switching back to system', () => {
    applyTheme('dark');
    applyTheme('system');

    expect(document.documentElement).not.toHaveAttribute('data-theme');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
    expect(getStoredTheme()).toBe('system');
  });

  it('ignores an invalid stored value', () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, 'neon');

    expect(getStoredTheme()).toBe('system');
  });

  it('still applies the theme when storage throws', () => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = () => {
      throw new Error('blocked');
    };
    try {
      expect(() => applyTheme('dark')).not.toThrow();
      expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    } finally {
      Storage.prototype.setItem = original;
    }
  });
});
