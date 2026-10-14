import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { LandingPage } from './LandingPage';

class IntersectionObserverMock implements IntersectionObserver {
  readonly root = null;
  readonly rootMargin = '0px';
  readonly scrollMargin = '0px';
  readonly thresholds = [];

  disconnect() {}
  observe() {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
  unobserve() {}
}

vi.stubGlobal('IntersectionObserver', IntersectionObserverMock);

describe('LandingPage', () => {
  it('shows the community hero and primary account actions', () => {
    const { container } = render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole('img', { name: 'Friends making plans together in a city plaza' }),
    ).toBeVisible();
    expect(screen.getAllByRole('link', { name: 'Create an account' })).not.toHaveLength(0);
    expect(screen.getAllByRole('link', { name: 'Log in' })).not.toHaveLength(0);
    expect(
      screen.getByRole('heading', { name: /Make plans\.\s*Meet people nearby\./ }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'How it works' })).toBeVisible();
    expect(screen.getByText('Amina')).toBeVisible();
    expect(screen.queryByText(/not dating/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/chat room/i)).not.toBeInTheDocument();
    expect(container).not.toHaveTextContent(/[-–—]/);
  });
});
