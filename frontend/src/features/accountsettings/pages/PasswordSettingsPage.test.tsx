import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { PasswordSettingsPage } from './PasswordSettingsPage';

describe('PasswordSettingsPage', () => {
  it('shows the password form under a Back to Settings link', () => {
    renderWithProviders(<PasswordSettingsPage />);

    expect(screen.getByRole('link', { name: /Back to Settings/ })).toHaveAttribute(
      'href',
      '/settings',
    );
    expect(screen.getByRole('heading', { name: 'Password' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change password' })).toBeInTheDocument();
  });
});
