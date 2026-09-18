import type { LocalFeedScope } from './contracts';

export const invitePostQueryKeys = {
  all: ['invite-posts'] as const,
  localFeed: (scope?: LocalFeedScope) => ['invite-posts', 'local', scope ?? 'all'] as const,
  globalFeed: ['invite-posts', 'global'] as const,
};
