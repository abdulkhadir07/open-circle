export const ratingQueryKeys = {
  due: ['ratings', 'due'] as const,
  received: ['ratings', 'received'] as const,
  reputation: (userId: string) => ['ratings', 'reputation', userId] as const,
};
