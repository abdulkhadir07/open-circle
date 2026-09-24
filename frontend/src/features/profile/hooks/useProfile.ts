import { useQuery } from '@tanstack/react-query';
import { getProfile } from '../api/profileApi';
import { profileQueryKeys } from '../api/queryKeys';

export function useProfile(userId: string | undefined) {
  return useQuery({
    queryKey: profileQueryKeys.detail(userId ?? ''),
    queryFn: () => getProfile(userId as string),
    enabled: Boolean(userId),
  });
}
