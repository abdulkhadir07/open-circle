export const scoreQueryKeys = {
  mine: ['score', 'mine'] as const,
  board: ['score', 'board'] as const,
  award: (year: number) => ['score', 'award', year] as const,
};
