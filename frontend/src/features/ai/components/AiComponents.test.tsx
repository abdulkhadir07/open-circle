import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/lib/api/errors';
import { AiBadge } from './AiBadge';
import { ContentError } from './ContentError';
import { FeedDigestCard } from './FeedDigestCard';
import { SafetyBlockedNotice } from './SafetyBlockedNotice';

function apiError(status: number, message: string) {
  return new ApiError({
    timestamp: new Date().toISOString(),
    status,
    error: 'X',
    message,
    path: '/x',
    fieldErrors: {},
  });
}

describe('SafetyBlockedNotice', () => {
  it('names Safety Guardian and says what to change', () => {
    render(<SafetyBlockedNotice message="Please keep payments out of OpenCircle." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Safety Guardian paused this');
    expect(screen.getByText('Please keep payments out of OpenCircle.')).toBeInTheDocument();
  });
});

describe('ContentError', () => {
  it('shows nothing without an error', () => {
    render(<ContentError error={null} fallback="Oops" />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows the Safety Guardian notice for a refused post', () => {
    render(<ContentError error={apiError(422, 'No threats please.')} fallback="Oops" />);

    expect(screen.getByText('Safety Guardian paused this')).toBeInTheDocument();
    expect(screen.getByText('No threats please.')).toBeInTheDocument();
  });

  it('shows plain error text for any other failure', () => {
    render(<ContentError error={apiError(500, 'Server broke')} fallback="Oops" />);

    expect(screen.getByRole('alert')).toHaveTextContent('Server broke');
    expect(screen.queryByText('Safety Guardian paused this')).not.toBeInTheDocument();
  });

  it('uses the fallback text for a non-Error value', () => {
    render(<ContentError error="weird" fallback="Unable to post." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Unable to post.');
  });
});

describe('FeedDigestCard', () => {
  it('shows the digest, with an AI marker only when the model wrote it', () => {
    const { unmount } = render(<FeedDigestCard digest="3 open invites today." aiGenerated />);
    expect(screen.getByText('3 open invites today.')).toBeInTheDocument();
    expect(screen.getByText('AI')).toBeInTheDocument();
    unmount();

    render(<FeedDigestCard digest="3 open invites today." aiGenerated={false} />);
    expect(screen.queryByText('AI')).not.toBeInTheDocument();
  });
});

describe('AiBadge', () => {
  it('reads "AI"', () => {
    render(<AiBadge />);

    expect(screen.getByText('AI')).toBeInTheDocument();
  });
});
