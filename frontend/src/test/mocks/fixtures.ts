import type { AuthUser } from '@/features/auth/api/contracts';
import type { InvitePost } from '@/features/invite-posts/api/contracts';

export const authUser: AuthUser = {
  id: '11111111-1111-4111-8111-111111111111',
  username: 'maya.chen',
  firstName: 'Maya',
  lastName: 'Chen',
  email: 'maya@example.com',
  phoneNumber: '+1 415 555 0100',
  dateOfBirth: '1994-05-12',
  city: 'San Francisco',
  stateRegion: 'California',
  country: 'United States',
  verifiedCity: 'San Francisco',
  verifiedStateRegion: 'California',
  verifiedCountry: 'United States',
  locationVerifiedAt: '2026-01-01T00:00:00Z',
  locationSource: 'DEVICE',
  role: 'USER',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

export const invitePost: InvitePost = {
  id: '22222222-2222-4222-8222-222222222222',
  posterId: authUser.id,
  posterUsername: authUser.username,
  content: 'Anyone up for coffee near Union Square this afternoon?',
  inviteType: 'SINGLE',
  totalCapacity: 1,
  acceptedCount: 0,
  invitesLeft: 1,
  locationScope: 'CITY',
  city: 'San Francisco',
  stateRegion: 'California',
  country: 'United States',
  status: 'ACTIVE',
  // Relative to "now" rather than a fixed date, since the countdown on each
  // card is computed live from this — a hardcoded past date would render
  // as "Expired" no matter when the tests happen to run.
  expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};
