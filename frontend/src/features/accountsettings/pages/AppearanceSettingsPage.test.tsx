import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { AppearanceSettingsPage } from './AppearanceSettingsPage';

afterEach(() => {
  window.localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
});

describe('AppearanceSettingsPage', () => {
  it('links back to Settings and defaults to System', () => {
    renderWithProviders(<AppearanceSettingsPage />);

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('radio', { name: /System/ })).toBeChecked();
  });

  it('applies and remembers the chosen theme immediately', async () => {
    const { user } = renderWithProviders(<AppearanceSettingsPage />);

    await user.click(screen.getByRole('radio', { name: /Dark/ }));

    expect(screen.getByRole('radio', { name: /Dark/ })).toBeChecked();
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(window.localStorage.getItem('opencircle-theme')).toBe('dark');
  });

  it('returns to following the device when System is chosen again', async () => {
    const { user } = renderWithProviders(<AppearanceSettingsPage />);

    await user.click(screen.getByRole('radio', { name: /Light/ }));
    await user.click(screen.getByRole('radio', { name: /System/ }));

    expect(document.documentElement).not.toHaveAttribute('data-theme');
    expect(window.localStorage.getItem('opencircle-theme')).toBeNull();
  });

  it('starts on the previously saved choice', () => {
    window.localStorage.setItem('opencircle-theme', 'light');

    renderWithProviders(<AppearanceSettingsPage />);

    expect(screen.getByRole('radio', { name: /Light/ })).toBeChecked();
  });
});
