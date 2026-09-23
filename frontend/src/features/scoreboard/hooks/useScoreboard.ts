import { useQuery } from '@tanstack/react-query';
import { getScoreboard } from '../api/scoreApi';
import { scoreQueryKeys } from '../api/queryKeys';

export function useScoreboard() {
  return useQuery({
    queryKey: scoreQueryKeys.board,
    queryFn: getScoreboard,
  });
}
