import { useQuery } from '@tanstack/react-query';
import { getReputation } from '../api/ratingsApi';
import { ratingQueryKeys } from '../api/queryKeys';

export function useReputation(userId: string | undefined) {
  return useQuery({
    queryKey: ratingQueryKeys.reputation(userId ?? ''),
    queryFn: () => getReputation(userId as string),
    enabled: Boolean(userId),
  });
}
