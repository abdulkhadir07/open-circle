import { useQuery } from '@tanstack/react-query';
import { getMyScore } from '../api/scoreApi';
import { scoreQueryKeys } from '../api/queryKeys';

export function useMyScore() {
  return useQuery({
    queryKey: scoreQueryKeys.mine,
    queryFn: getMyScore,
  });
}
