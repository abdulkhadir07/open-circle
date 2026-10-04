import { useQuery } from '@tanstack/react-query';
import { getCampusFeed } from '../api/invitePostsApi';
import { invitePostQueryKeys } from '../api/queryKeys';

export function useCampusFeed() {
  return useQuery({
    queryKey: invitePostQueryKeys.campusFeed,
    queryFn: getCampusFeed,
  });
}
