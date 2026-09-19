import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { engagementRequest } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { EngagementRequestsPage } from './EngagementRequestsPage';

describe('EngagementRequestsPage', () => {
  it('links back to the home page', async () => {
    renderWithProviders(<EngagementRequestsPage />);

    expect(screen.getByRole('link', { name: 'Back' })).toHaveAttribute('href', '/');
  });

  it('shows Received by default and switches to Sent', async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
      http.get('*/api/engagements/mine', () => HttpResponse.json([])),
    );
    const { user } = renderWithProviders(<EngagementRequestsPage />);

    expect(await screen.findByText(engagementRequest.requesterUsername)).toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: 'Sent' }));

    expect(
      await screen.findByText("You haven't requested to join anything yet."),
    ).toBeInTheDocument();
    expect(screen.queryByText(engagementRequest.requesterUsername)).not.toBeInTheDocument();
  });
});
