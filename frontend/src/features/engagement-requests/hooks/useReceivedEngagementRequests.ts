import { useQuery } from '@tanstack/react-query';
import { getReceivedEngagementRequests } from '../api/engagementRequestsApi';
import { engagementRequestQueryKeys } from '../api/queryKeys';

export function useReceivedEngagementRequests(options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: engagementRequestQueryKeys.received,
    queryFn: getReceivedEngagementRequests,
    enabled: options.enabled ?? true,
  });
}
