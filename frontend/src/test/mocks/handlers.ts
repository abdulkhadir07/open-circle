import { http, HttpResponse } from 'msw';
import { authUser, chatMessage, chatRoom, engagementRequest, invitePost } from './fixtures';
import type { Reputation } from '@/features/ratings/api/contracts';
import type { ScoreSummary } from '@/features/scoreboard/api/contracts';

export const handlers = [
  http.post('*/api/auth/refresh', () => HttpResponse.json({ token: 'refreshed-token' })),
  http.get('*/api/users/me', () => HttpResponse.json(authUser)),
  http.post('*/api/auth/login', () => HttpResponse.json({ token: 'login-token', user: authUser })),
  http.post('*/api/auth/signup', () => HttpResponse.json({ user: authUser }, { status: 201 })),
  http.post('*/api/auth/verify-email', () =>
    HttpResponse.json({ token: 'verified-token', user: authUser }),
  ),
  http.post('*/api/auth/resend-verification', () => new HttpResponse(null, { status: 204 })),
  http.post('*/api/auth/logout', () => new HttpResponse(null, { status: 204 })),
  http.put('*/api/users/me/location', () => HttpResponse.json(authUser)),
  http.post('*/api/invite-posts', () => HttpResponse.json(invitePost, { status: 201 })),
  http.get('*/api/invite-posts/local', () => HttpResponse.json([invitePost])),
  http.get('*/api/invite-posts/global', () => HttpResponse.json([])),
  http.post('*/api/invite-posts/:postId/engagements', () =>
    HttpResponse.json(engagementRequest, { status: 201 }),
  ),
  http.get('*/api/engagements/mine', () => HttpResponse.json([])),
  http.get('*/api/engagements/received', () => HttpResponse.json([])),
  http.patch('*/api/engagements/:requestId/accept', () =>
    HttpResponse.json({ ...engagementRequest, status: 'ACCEPTED' }),
  ),
  http.patch('*/api/engagements/:requestId/decline', () =>
    HttpResponse.json({ ...engagementRequest, status: 'DECLINED' }),
  ),
  http.patch('*/api/engagements/:requestId/hold', () =>
    HttpResponse.json({ ...engagementRequest, status: 'HELD' }),
  ),
  http.patch('*/api/engagements/:requestId/withdraw', () =>
    HttpResponse.json({ ...engagementRequest, status: 'WITHDRAWN' }),
  ),
  http.get('*/api/chat-rooms', () => HttpResponse.json([])),
  http.get('*/api/chat-rooms/hidden', () => HttpResponse.json([])),
  http.get('*/api/chat-rooms/:roomId/messages', () => HttpResponse.json([])),
  http.post('*/api/chat-rooms/:roomId/messages', () =>
    HttpResponse.json(chatMessage, { status: 201 }),
  ),
  http.patch('*/api/chat-rooms/:roomId/save', () =>
    HttpResponse.json({ ...chatRoom, saved: true }),
  ),
  http.patch('*/api/chat-rooms/:roomId/leave', () => HttpResponse.json(chatRoom)),
  http.patch('*/api/chat-rooms/:roomId/hide', () => HttpResponse.json(chatRoom)),
  http.patch('*/api/chat-rooms/:roomId/unhide', () => HttpResponse.json(chatRoom)),
  http.put('*/api/users/me/hidden-chats-pin', () => new HttpResponse(null, { status: 204 })),
  http.get('*/api/notifications', () =>
    HttpResponse.json({ notifications: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
  ),
  http.get('*/api/notifications/unread-count', () => HttpResponse.json({ unreadCount: 0 })),
  http.patch(
    '*/api/notifications/:notificationId/read',
    () => new HttpResponse(null, { status: 204 }),
  ),
  http.patch('*/api/notifications/read-all', () => new HttpResponse(null, { status: 204 })),
  http.get('*/api/users/me/ratings/due', () => HttpResponse.json([])),
  http.post('*/api/engagements/:engagementId/ratings', ({ params }) =>
    HttpResponse.json(
      {
        id: 'rating-id',
        engagementId: params.engagementId as string,
        ratedUserId: engagementRequest.requesterId,
        score: 5,
        submittedAt: new Date().toISOString(),
        revealed: false,
      },
      { status: 201 },
    ),
  ),
  http.get('*/api/users/me/ratings/received', () =>
    HttpResponse.json({ ratings: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
  ),
  http.get('*/api/users/:userId/reputation', ({ params }) =>
    HttpResponse.json({
      userId: params.userId as string,
      username: 'user',
      profileImage: null,
      averageRating: null,
      totalRatingsReceived: 0,
      distinctRaterCount: 0,
    } satisfies Reputation),
  ),
  http.get('*/api/users/me/score', () =>
    HttpResponse.json({
      userId: authUser.id,
      seasonYear: new Date().getUTCFullYear(),
      annualScore: 0,
      lifetimeScore: 0,
    } satisfies ScoreSummary),
  ),
  http.get('*/api/scoreboard', () =>
    HttpResponse.json({ seasonYear: new Date().getUTCFullYear(), entries: [] }),
  ),
];
