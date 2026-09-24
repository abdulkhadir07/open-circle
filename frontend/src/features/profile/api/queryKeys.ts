export const profileQueryKeys = {
  detail: (userId: string) => ['profile', userId] as const,
};
