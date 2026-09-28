import { useQuery } from '@tanstack/react-query';
import { getAnnualAward } from '../api/scoreApi';
import { scoreQueryKeys } from '../api/queryKeys';

export function useAnnualAward(year: number) {
  return useQuery({
    queryKey: scoreQueryKeys.award(year),
    queryFn: () => getAnnualAward(year),
    retry: false,
  });
}
