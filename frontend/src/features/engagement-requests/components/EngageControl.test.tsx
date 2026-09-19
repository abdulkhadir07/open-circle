import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { engagementRequest, invitePost } from '@/test/mocks/fixtures';
import { server } from '@/test/mocks/server';
import { renderWithProviders } from '@/test/render';
import { EngageControl } from './EngageControl';

describe('EngageControl', () => {
  it('shows an Engage button when there is no existing request and the post is open', () => {
    renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={undefined} postOpen={true} />,
    );

    expect(screen.getByRole('button', { name: 'Engage' })).toBeInTheDocument();
  });

  it('renders nothing when there is no existing request and the post is not open', () => {
    const { container } = renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={undefined} postOpen={false} />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('sends a request when clicked, with no error on success', async () => {
    const { user } = renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={undefined} postOpen={true} />,
    );

    await user.click(screen.getByRole('button', { name: 'Engage' }));

    // This component only reflects the request via its `myRequest` prop —
    // whether it flips to "Request sent" is HomePage's job (it refetches
    // "mine" and re-passes the prop down), covered in HomePage.test.tsx.
    // Here we just confirm the mutation itself succeeds cleanly.
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Engage' })).not.toBeDisabled();
  });

  it('shows an error if the request fails', async () => {
    server.use(
      http.post('*/api/invite-posts/:postId/engagements', () =>
        HttpResponse.json(
          {
            timestamp: new Date().toISOString(),
            status: 409,
            error: 'CONFLICT',
            message: 'You have already requested to engage with this post',
            path: '/api/invite-posts/mock/engagements',
            fieldErrors: {},
          },
          { status: 409 },
        ),
      ),
    );
    const { user } = renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={undefined} postOpen={true} />,
    );

    await user.click(screen.getByRole('button', { name: 'Engage' }));

    expect(
      await screen.findByText('You have already requested to engage with this post'),
    ).toBeInTheDocument();
  });

  it('shows Request sent with a Withdraw action for a pending request', () => {
    renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={engagementRequest} postOpen={true} />,
    );

    expect(screen.getByText('Request sent')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Withdraw Request' })).toBeInTheDocument();
  });

  it('withdraws a pending request when Withdraw is clicked, with no error on success', async () => {
    const { user } = renderWithProviders(
      <EngageControl invitePostId={invitePost.id} myRequest={engagementRequest} postOpen={true} />,
    );

    await user.click(screen.getByRole('button', { name: 'Withdraw Request' }));

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Withdraw Request' })).not.toBeDisabled();
  });

  it('shows On hold with a Withdraw action for a held request', () => {
    renderWithProviders(
      <EngageControl
        invitePostId={invitePost.id}
        myRequest={{ ...engagementRequest, status: 'HELD' }}
        postOpen={true}
      />,
    );

    expect(screen.getByText('On hold')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Withdraw Request' })).toBeInTheDocument();
  });

  it('shows a final status with no action for an accepted request', () => {
    renderWithProviders(
      <EngageControl
        invitePostId={invitePost.id}
        myRequest={{ ...engagementRequest, status: 'ACCEPTED' }}
        postOpen={true}
      />,
    );

    expect(screen.getByText("You're in")).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Withdraw Request' })).not.toBeInTheDocument();
  });

  it('shows a final status with no action for a declined request', () => {
    renderWithProviders(
      <EngageControl
        invitePostId={invitePost.id}
        myRequest={{ ...engagementRequest, status: 'DECLINED' }}
        postOpen={true}
      />,
    );

    expect(screen.getByText('Declined')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Withdraw Request' })).not.toBeInTheDocument();
  });
});
