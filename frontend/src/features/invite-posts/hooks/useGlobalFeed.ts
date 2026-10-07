import { useQuery } from '@tanstack/react-query';
import { getGlobalFeed } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';

export function useGlobalFeed(options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: invitePostQueryKeys.globalFeed,
    queryFn: getGlobalFeed,
    enabled: options.enabled ?? true,
  });
}
