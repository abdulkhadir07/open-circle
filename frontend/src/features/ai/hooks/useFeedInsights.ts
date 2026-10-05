import { useQuery } from '@tanstack/react-query';
import { getFeedInsights } from '../api/aiApi';
import { aiQueryKeys } from '../api/queryKeys';

const TEN_MINUTES_MS = 10 * 60 * 1000;

/**
 * The Home digest. Optional by nature: it never retries, doesn't refetch on focus (each fetch can be
 * an AI call), and callers should show nothing when it fails.
 */
export function useFeedInsights(enabled: boolean) {
  return useQuery({
    queryKey: aiQueryKeys.feedInsights,
    queryFn: getFeedInsights,
    enabled,
    staleTime: TEN_MINUTES_MS,
    refetchOnWindowFocus: false,
    retry: false,
  });
}
