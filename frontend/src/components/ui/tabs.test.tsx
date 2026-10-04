import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/render';
import { Tabs } from './tabs';

const ITEMS = [
  { key: 'received', to: '/requests?tab=received', label: 'Received' },
  { key: 'sent', to: '/requests?tab=sent', label: 'Sent' },
];

describe('Tabs', () => {
  it('links each tab and marks only the active one as the current page', () => {
    renderWithProviders(<Tabs items={ITEMS} active="sent" />);

    expect(screen.getByRole('link', { name: 'Sent' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Received' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('link', { name: 'Received' })).toHaveAttribute(
      'href',
      '/requests?tab=received',
    );
  });
});
