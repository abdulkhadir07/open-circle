import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { accountSessions } from '@/test/mocks/fixtures';
import { renderWithProviders } from '@/test/render';
import { SessionRow } from './SessionRow';

const currentSession = accountSessions[0]!;
const otherSession = accountSessions[1]!;

describe('SessionRow', () => {
  it("shows a 'This device' badge and no Revoke button for the current session", () => {
    renderWithProviders(
      <SessionRow session={currentSession} onRevoke={vi.fn<() => void>()} revoking={false} />,
    );

    expect(screen.getByText('This device')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /revoke/i })).not.toBeInTheDocument();
  });

  it('shows a Revoke button for a non-current session and calls onRevoke when clicked', async () => {
    const onRevoke = vi.fn<() => void>();
    const { user } = renderWithProviders(
      <SessionRow session={otherSession} onRevoke={onRevoke} revoking={false} />,
    );

    expect(screen.queryByText('This device')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Revoke' }));

    expect(onRevoke).toHaveBeenCalledOnce();
  });

  it('shows the parsed device label', () => {
    renderWithProviders(
      <SessionRow session={otherSession} onRevoke={vi.fn<() => void>()} revoking={false} />,
    );

    expect(screen.getByText(/Safari on iOS/)).toBeInTheDocument();
  });
});
