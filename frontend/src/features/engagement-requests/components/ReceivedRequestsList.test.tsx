import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { engagementRequest, reputation } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { ReceivedRequestsList } from './ReceivedRequestsList';

describe('ReceivedRequestsList', () => {
  it('shows an empty state when nothing has been received', async () => {
    renderWithProviders(<ReceivedRequestsList />);

    expect(
      await screen.findByText('No one has requested to join your posts yet.'),
    ).toBeInTheDocument();
  });

  it('lists a received request with the requester, post content, and actions', async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
    );
    renderWithProviders(<ReceivedRequestsList />);

    expect(await screen.findByText(engagementRequest.requesterUsername)).toBeInTheDocument();
    expect(screen.getByText(engagementRequest.invitePost.content)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Accept' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Decline' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hold' })).toBeInTheDocument();
  });

  it('offers Accept and Decline but not Hold for an already-held request', async () => {
    server.use(
      http.get('*/api/engagements/received', () =>
        HttpResponse.json([{ ...engagementRequest, status: 'HELD' }]),
      ),
    );
    renderWithProviders(<ReceivedRequestsList />);

    expect(await screen.findByText('On hold')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Accept' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hold' })).not.toBeInTheDocument();
  });

  it('does not offer actions for a final-state request', async () => {
    server.use(
      http.get('*/api/engagements/received', () =>
        HttpResponse.json([{ ...engagementRequest, status: 'DECLINED' }]),
      ),
    );
    renderWithProviders(<ReceivedRequestsList />);

    await screen.findByText(engagementRequest.requesterUsername);
    expect(screen.queryByRole('button', { name: 'Accept' })).not.toBeInTheDocument();
  });

  it('accepts a request when Accept is clicked, with no error on success', async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
    );
    const { user } = renderWithProviders(<ReceivedRequestsList />);

    await user.click(await screen.findByRole('button', { name: 'Accept' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });

  it('shows an error if accepting fails', async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
      http.patch('*/api/engagements/:requestId/accept', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 400,
            error: 'BAD_REQUEST',
            message: 'Engagement request is not actionable',
            path: '/api/engagements/mock/accept',
            fieldErrors: {},
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderWithProviders(<ReceivedRequestsList />);

    await user.click(await screen.findByRole('button', { name: 'Accept' }));

    expect(await screen.findByText('Engagement request is not actionable')).toBeInTheDocument();
  });

  it("shows the requester's reputation badge once loaded", async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
      http.get('*/api/users/:userId/reputation', () =>
        HttpResponse.json({ ...reputation, userId: engagementRequest.requesterId }),
      ),
    );
    renderWithProviders(<ReceivedRequestsList />);

    expect(await screen.findByText('4.8/5')).toBeInTheDocument();
  });

  it("links the requester's name to their profile", async () => {
    server.use(
      http.get('*/api/engagements/received', () => HttpResponse.json([engagementRequest])),
    );
    renderWithProviders(<ReceivedRequestsList />);

    expect(
      await screen.findByRole('link', { name: engagementRequest.requesterUsername }),
    ).toHaveAttribute('href', `/profile/${engagementRequest.requesterId}`);
  });
});
