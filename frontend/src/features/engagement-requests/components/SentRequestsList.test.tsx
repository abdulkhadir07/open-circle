import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { engagementRequest } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { SentRequestsList } from './SentRequestsList';

describe('SentRequestsList', () => {
  it('shows an empty state when nothing has been sent', async () => {
    renderWithProviders(<SentRequestsList />);

    expect(
      await screen.findByText("You haven't requested to join anything yet."),
    ).toBeInTheDocument();
  });

  it('lists a sent request with the post content, poster, and a Withdraw action', async () => {
    server.use(http.get('*/api/engagements/mine', () => HttpResponse.json([engagementRequest])));
    renderWithProviders(<SentRequestsList />);

    expect(
      await screen.findByText(engagementRequest.invitePost.posterUsername),
    ).toBeInTheDocument();
    expect(screen.getByText(engagementRequest.invitePost.content)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Withdraw Request' })).toBeInTheDocument();
  });

  it('does not offer Withdraw for a final-state request', async () => {
    server.use(
      http.get('*/api/engagements/mine', () =>
        HttpResponse.json([{ ...engagementRequest, status: 'ACCEPTED' }]),
      ),
    );
    renderWithProviders(<SentRequestsList />);

    await screen.findByText(engagementRequest.invitePost.posterUsername);
    expect(screen.queryByRole('button', { name: 'Withdraw Request' })).not.toBeInTheDocument();
  });

  it('withdraws a request when Withdraw is clicked, with no error on success', async () => {
    server.use(http.get('*/api/engagements/mine', () => HttpResponse.json([engagementRequest])));
    const { user } = renderWithProviders(<SentRequestsList />);

    await user.click(await screen.findByRole('button', { name: 'Withdraw Request' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });

  it('shows an error if loading fails', async () => {
    server.use(
      http.get('*/api/engagements/mine', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Something went wrong',
            path: '/api/engagements/mine',
            fieldErrors: {},
          },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<SentRequestsList />);

    expect(await screen.findByText('Something went wrong')).toBeInTheDocument();
  });
});
