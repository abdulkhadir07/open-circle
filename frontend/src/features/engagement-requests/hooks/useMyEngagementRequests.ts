import { useQuery } from '@tanstack/react-query';
import { getMyEngagementRequests } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useMyEngagementRequests(options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: engagementRequestQueryKeys.mine,
    queryFn: getMyEngagementRequests,
    enabled: options.enabled ?? true,
  });
}
