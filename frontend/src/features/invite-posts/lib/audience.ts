import type { AuthUser } from '@/features/auth/api/contracts';
import type { LocationScope } from '../api/contracts';

/** What the Home feed is showing: everything local ("NEARBY"), one scope, or the global feed. */
export type FeedAudience = 'NEARBY' | LocationScope;

type VerifiedPlace = Pick<AuthUser, 'verifiedCity' | 'verifiedStateRegion' | 'verifiedCountry'>;

const STORAGE_KEY = 'opencircle.feed-audience';

/** Scopes named after the viewer's verified place, e.g. { CITY: 'Austin', GLOBAL: 'Worldwide' }. */
export function scopeNames(user: VerifiedPlace): Record<LocationScope, string> {
  return {
    CITY: user.verifiedCity || 'Your city',
    STATE_REGION: user.verifiedStateRegion || 'Your state',
    COUNTRY: user.verifiedCountry || 'Your country',
    GLOBAL: 'Worldwide',
  };
}

/** A scope only exists for the viewer when their place has it (not every country has states). */
export function availableScopes(user: VerifiedPlace): LocationScope[] {
  return (['CITY', 'STATE_REGION', 'COUNTRY', 'GLOBAL'] as const).filter(
    (scope) => scope !== 'STATE_REGION' || Boolean(user.verifiedStateRegion),
  );
}

export function feedAudienceOptions(user: VerifiedPlace): { value: FeedAudience; label: string }[] {
  const names = scopeNames(user);
  return [
    { value: 'NEARBY', label: 'Nearby' },
    ...availableScopes(user).map((scope) => ({ value: scope, label: names[scope] })),
  ];
}

/** The audience picker on the new-invite page: the same place names, with who that reaches. */
export function postAudienceOptions(
  user: VerifiedPlace,
): { value: LocationScope; label: string }[] {
  const names = scopeNames(user);
  const suffix: Record<LocationScope, string> = {
    CITY: 'your city',
    STATE_REGION: 'your state',
    COUNTRY: 'your country',
    GLOBAL: 'anyone',
  };
  return availableScopes(user).map((scope) => ({
    value: scope,
    label: `${names[scope]} · ${suffix[scope]}`,
  }));
}

/** "Austin, Texas, United States" — skips whatever the place doesn't have. */
export function placeLine(user: VerifiedPlace): string {
  return [user.verifiedCity, user.verifiedStateRegion, user.verifiedCountry]
    .filter(Boolean)
    .join(', ');
}

export function isFeedAudience(value: unknown): value is FeedAudience {
  return (
    value === 'NEARBY' ||
    value === 'CITY' ||
    value === 'STATE_REGION' ||
    value === 'COUNTRY' ||
    value === 'GLOBAL'
  );
}

/** The last audience the person picked on this device, if any. Storage can be blocked. */
export function readStoredAudience(): FeedAudience | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return isFeedAudience(stored) ? stored : null;
  } catch {
    return null;
  }
}

export function storeAudience(audience: FeedAudience) {
  try {
    window.localStorage.setItem(STORAGE_KEY, audience);
  } catch {
    // Remembering the choice is a convenience; ignore blocked storage.
  }
}
