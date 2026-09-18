import { http, HttpResponse } from 'msw';
import { authUser, invitePost } from './fixtures';

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
];
