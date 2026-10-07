import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { authUser } from '@/test/mocks/fixtures';
import { createTestQueryClient, renderWithProviders } from '@/test/render';
import { PlaceLabel } from './PlaceLabel';

describe('PlaceLabel', () => {
  beforeEach(() => {
    Reflect.deleteProperty(navigator, 'geolocation');
  });

  it('shows the place and no update button when geolocation is unsupported', () => {
    renderWithProviders(<PlaceLabel place="Austin, Texas, United States" />);

    expect(screen.getByText('Austin, Texas, United States')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Update' })).not.toBeInTheDocument();
  });

  it('re-verifies the location and refreshes the cached user', async () => {
    Object.defineProperty(navigator, 'geolocation', {
      value: {
        getCurrentPosition: (success: PositionCallback) =>
          success({ coords: { latitude: 30.27, longitude: -97.74 } } as GeolocationPosition),
      },
      configurable: true,
    });
    const queryClient = createTestQueryClient();
    const { user } = renderWithProviders(<PlaceLabel place="Austin, Texas, United States" />, {
      queryClient,
    });

    await user.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => {
      expect(queryClient.getQueryData(authQueryKeys.currentUser)).toEqual(authUser);
    });
  });

  it('explains when location access is blocked', async () => {
    Object.defineProperty(navigator, 'geolocation', {
      value: {
        getCurrentPosition: (_success: PositionCallback, error: PositionErrorCallback) =>
          error({ code: 1, message: 'denied' } as GeolocationPositionError),
      },
      configurable: true,
    });
    const { user } = renderWithProviders(<PlaceLabel place="Austin" />);

    await user.click(screen.getByRole('button', { name: 'Update' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/location access was blocked/i);
  });
});
