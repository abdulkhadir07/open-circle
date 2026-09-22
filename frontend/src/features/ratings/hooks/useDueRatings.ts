import { useQuery } from '@tanstack/react-query';
import { getDueRatings } from '../api/ratingsApi';
import { ratingQueryKeys } from '../api/queryKeys';

export function useDueRatings() {
  return useQuery({
    queryKey: ratingQueryKeys.due,
    queryFn: getDueRatings,
  });
}
