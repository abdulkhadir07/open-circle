import { useQuery } from '@tanstack/react-query';
import { getLocalFeed } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';
import type { LocalFeedScope } from '../api/contracts';

export function useLocalFeed(scope?: LocalFeedScope, options: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: invitePostQueryKeys.localFeed(scope),
    queryFn: () => getLocalFeed(scope),
    enabled: options.enabled ?? true,
  });
}
