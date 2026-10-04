import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { engagementRequest } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { EngagementRequestsPage } from './EngagementRequestsPage';

function renderPage(path = '/requests') {
  return renderWithProviders(
    <Routes>
      <Route path="/requests" element={<EngagementRequestsPage />} />
    </Routes>,
    { initialEntries: [path] },
  );
}

describe('EngagementRequestsPage', () => {
  it('shows Received by default and links each tab', async () => {
    renderPage();

    expect(screen.getByRole('link', { name: 'Received' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Sent' })).toHaveAttribute(
      'href',
      '/requests?tab=sent',
    );
  });

  it('shows Received by default and switches to Sent', async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
      http.get('*/api/engagements/mine', () => HttpResponse.json([])),
    );
    const { user } = renderPage();

    expect(await screen.findByText(engagementRequest.requesterUsername)).toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Sent' }));

    expect(
      await screen.findByText("You haven't requested to join anything yet."),
    ).toBeInTheDocument();
    expect(screen.queryByText(engagementRequest.requesterUsername)).not.toBeInTheDocument();
  });

  it('opens straight to the Sent tab from a ?tab=sent link', async () => {
    renderPage('/requests?tab=sent');

    expect(screen.getByRole('link', { name: 'Sent' })).toHaveAttribute('aria-current', 'page');
    expect(
      await screen.findByText("You haven't requested to join anything yet."),
    ).toBeInTheDocument();
  });
});
