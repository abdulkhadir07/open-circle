import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { LocationVerificationPrompt } from './LocationVerificationPrompt';

function mockGeolocation(getCurrentPosition: (typeof navigator.geolocation)['getCurrentPosition']) {
  Object.defineProperty(navigator, 'geolocation', {
    value: { getCurrentPosition },
    configurable: true,
  });
}

describe('LocationVerificationPrompt', () => {
  beforeEach(() => {
    Reflect.deleteProperty(navigator, 'geolocation');
  });

  it('shows a message instead of a button when geolocation is unsupported', () => {
    renderWithProviders(<LocationVerificationPrompt />);

    expect(screen.getByText(/doesn't support location access/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Share my location' })).not.toBeInTheDocument();
  });

  it('verifies the location and updates the cached user on success', async () => {
    mockGeolocation((success) => {
      success({
        coords: { latitude: 37.7749, longitude: -122.4194 },
      } as GeolocationPosition);
    });
    const queryClient = createTestQueryClient();
    const { user } = renderWithProviders(<LocationVerificationPrompt />, { queryClient });

    await user.click(screen.getByRole('button', { name: 'Share my location' }));

    await waitFor(() => {
      expect(queryClient.getQueryData(authQueryKeys.currentUser)).toEqual(authUser);
    });
  });

  it('shows a friendly message when location permission is denied', async () => {
    mockGeolocation((_success, error) => {
      error?.({ code: 1, message: 'denied' } as GeolocationPositionError);
    });
    const { user } = renderWithProviders(<LocationVerificationPrompt />);

    await user.click(screen.getByRole('button', { name: 'Share my location' }));

    expect(await screen.findByText(/location access was blocked/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Share my location' })).toBeEnabled();
  });
});
