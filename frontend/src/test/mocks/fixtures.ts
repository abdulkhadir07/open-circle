import type { AuthUser } from '@/features/auth/api/contracts';
import type { ChatMessage, ChatRoom } from '@/features/chat/api/contracts';
import type { EngagementRequest } from '@/features/engagement-requests/api/contracts';
import type { InvitePost } from '@/features/invite-posts/api/contracts';
import type { Notification } from '@/features/notifications/api/contracts';
import type { DueRating, ReceivedRating, Reputation } from '@/features/ratings/api/contracts';
import type { Scoreboard, ScoreSummary } from '@/features/scoreboard/api/contracts';
import type { UserProfile } from '@/features/profile/api/contracts';

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
  hasHiddenChatsPin: false,
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

export const engagementRequest: EngagementRequest = {
  id: '33333333-3333-4333-8333-333333333333',
  invitePostId: invitePost.id,
  invitePost: {
    id: invitePost.id,
    content: invitePost.content,
    posterUsername: invitePost.posterUsername,
    city: invitePost.city,
    stateRegion: invitePost.stateRegion,
    country: invitePost.country,
  },
  requesterId: '44444444-4444-4444-8444-444444444444',
  requesterUsername: 'sam.rivera',
  status: 'PENDING',
  expiresAt: invitePost.expiresAt,
  respondedAt: null,
  withdrawnAt: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

export const chatRoom: ChatRoom = {
  id: '55555555-5555-4555-8555-555555555555',
  invitePostId: invitePost.id,
  invitePostContent: invitePost.content,
  status: 'ACTIVE',
  saved: false,
  savedAt: null,
  savedByUserId: null,
  savedByUsername: null,
  savedByProfileImage: null,
  autoCloseAt: null,
  closed: false,
  closedAt: null,
  hiddenForCurrentUser: false,
  participants: [
    {
      userId: authUser.id,
      username: authUser.username,
      profileImage: null,
      active: true,
      joinedAt: new Date().toISOString(),
      left: false,
      leftAt: null,
      removed: false,
      removedAt: null,
      removedByUserId: null,
      removedByUsername: null,
      removedByProfileImage: null,
    },
    {
      userId: '66666666-6666-4666-8666-666666666666',
      username: 'jordan.lee',
      profileImage: null,
      active: true,
      joinedAt: new Date().toISOString(),
      left: false,
      leftAt: null,
      removed: false,
      removedAt: null,
      removedByUserId: null,
      removedByUsername: null,
      removedByProfileImage: null,
    },
  ],
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

export const chatMessage: ChatMessage = {
  id: '77777777-7777-4777-8777-777777777777',
  roomId: chatRoom.id,
  senderId: '66666666-6666-4666-8666-666666666666',
  senderUsername: 'jordan.lee',
  senderProfileImage: null,
  type: 'TEXT',
  body: 'See you at 3pm!',
  attachment: null,
  createdAt: new Date().toISOString(),
};

export const notification: Notification = {
  id: '88888888-8888-4888-8888-888888888888',
  type: 'ENGAGEMENT_REQUESTED',
  actor: {
    userId: engagementRequest.requesterId,
    username: engagementRequest.requesterUsername,
    profileImage: null,
  },
  resource: { type: 'ENGAGEMENT_REQUEST', id: engagementRequest.id },
  context: { type: 'INVITE_POST', id: invitePost.id },
  occurrenceCount: 1,
  occurredAt: new Date().toISOString(),
  read: false,
  readAt: null,
};

export const dueRating: DueRating = {
  obligationId: '99999999-9999-4999-8999-999999999999',
  engagementId: engagementRequest.id,
  otherUserId: engagementRequest.requesterId,
  otherUsername: engagementRequest.requesterUsername,
  otherUserProfileImage: null,
  trigger: 'PARTICIPANT_EXIT',
  requiredAt: new Date().toISOString(),
  dueAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
};

export const receivedRating: ReceivedRating = {
  id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  engagementId: engagementRequest.id,
  raterUserId: engagementRequest.requesterId,
  raterUsername: engagementRequest.requesterUsername,
  raterProfileImage: null,
  score: 5,
  revealedAt: new Date().toISOString(),
};

export const reputation: Reputation = {
  userId: authUser.id,
  username: authUser.username,
  profileImage: null,
  averageRating: 4.8,
  totalRatingsReceived: 12,
  distinctRaterCount: 10,
};

export const scoreSummary: ScoreSummary = {
  userId: authUser.id,
  seasonYear: 2026,
  annualScore: 42,
  lifetimeScore: 128,
};

export const scoreboard: Scoreboard = {
  seasonYear: 2026,
  entries: [
    {
      rank: 1,
      userId: authUser.id,
      username: authUser.username,
      profileImage: null,
      annualScore: 42,
      averageRating: 4.8,
      currentYearDistinctRaterCount: 10,
    },
    {
      rank: 2,
      userId: engagementRequest.requesterId,
      username: engagementRequest.requesterUsername,
      profileImage: null,
      annualScore: 30,
      averageRating: null,
      currentYearDistinctRaterCount: 3,
    },
  ],
};

export const userProfile: UserProfile = {
  userId: authUser.id,
  username: authUser.username,
  displayName: authUser.firstName,
  profileImage: null,
  bio: 'Always up for coffee and a good conversation.',
  interests: ['Hiking', 'Coffee', 'Live music'],
  memberSince: '2026-01-01T00:00:00Z',
  reputation: {
    averageRating: 4.8,
    totalRatingsReceived: 12,
    distinctRaterCount: 10,
  },
  awards: [
    {
      seasonYear: 2025,
      name: 'Circle Champion 2025',
      finalScore: 210,
      awardedAt: '2026-01-01T00:05:00Z',
    },
  ],
};
