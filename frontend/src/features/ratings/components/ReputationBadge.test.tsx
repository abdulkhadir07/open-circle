import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { reputation } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { ReputationBadge } from './ReputationBadge';

describe('ReputationBadge', () => {
  it('renders nothing while loading', () => {
    const { container } = renderWithProviders(<ReputationBadge userId={reputation.userId} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing for a user with no ratings yet', async () => {
    const { container } = renderWithProviders(<ReputationBadge userId="no-ratings-user" />);

    await waitFor(() => expect(container.firstChild).toBeNull());
  });

  it('renders nothing when userId is undefined', () => {
    const { container } = renderWithProviders(<ReputationBadge userId={undefined} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('shows the average score for a user with ratings', async () => {
    server.use(http.get('*/api/users/:userId/reputation', () => HttpResponse.json(reputation)));

    renderWithProviders(<ReputationBadge userId={reputation.userId} />);

    expect(await screen.findByText('4.8/5')).toBeInTheDocument();
  });

  it('includes the total count in the title for detail', async () => {
    server.use(http.get('*/api/users/:userId/reputation', () => HttpResponse.json(reputation)));

    renderWithProviders(<ReputationBadge userId={reputation.userId} />);

    expect(await screen.findByText('4.8/5')).toHaveAttribute('title', '4.8/5 average · 12 ratings');
  });
});
