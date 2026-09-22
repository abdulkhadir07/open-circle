import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { RateEngagementDialog } from './RateEngagementDialog';

describe('RateEngagementDialog', () => {
  it('disables submit until a score is picked', async () => {
    const { user } = renderWithProviders(
      <RateEngagementDialog engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: 'Rate' }));

    expect(screen.getByRole('button', { name: 'Submit rating' })).toBeDisabled();
  });

  it('submits the picked score and closes the dialog', async () => {
    const { user } = renderWithProviders(
      <RateEngagementDialog engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: 'Rate' }));
    await user.click(screen.getByRole('button', { name: '4 out of 5' }));
    await user.click(screen.getByRole('button', { name: 'Submit rating' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('resets the picked score when the dialog is cancelled and reopened', async () => {
    const { user } = renderWithProviders(
      <RateEngagementDialog engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: 'Rate' }));
    await user.click(screen.getByRole('button', { name: '4 out of 5' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    await user.click(screen.getByRole('button', { name: 'Rate' }));

    expect(screen.getByRole('button', { name: 'Submit rating' })).toBeDisabled();
  });

  it('shows an error and keeps the dialog open when submission fails', async () => {
    server.use(
      http.post('*/api/engagements/:engagementId/ratings', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 409,
            error: 'CONFLICT',
            message: 'Rating deadline has passed',
            path: '/api/engagements/engagement-1/ratings',
            fieldErrors: {},
          },
          { status: 409 },
        ),
      ),
    );
    const { user } = renderWithProviders(
      <RateEngagementDialog engagementId="engagement-1" otherUsername="jordan.lee" />,
    );

    await user.click(screen.getByRole('button', { name: 'Rate' }));
    await user.click(screen.getByRole('button', { name: '3 out of 5' }));
    await user.click(screen.getByRole('button', { name: 'Submit rating' }));

    expect(await screen.findByText('Rating deadline has passed')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});
