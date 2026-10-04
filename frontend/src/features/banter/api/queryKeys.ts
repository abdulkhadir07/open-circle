import type { BanterSort } from './contracts';

export const banterQueryKeys = {
  all: ['banter'] as const,
  lists: ['banter', 'list'] as const,
  list: (sort: BanterSort) => ['banter', 'list', sort] as const,
  replies: (banterId: string) => ['banter', 'replies', banterId] as const,
};
