import { useQuery } from '@tanstack/react-query';
import { getSessions } from '../api/accountSettingsApi';
import { accountSettingsQueryKeys } from '../api/queryKeys';

export function useSessions() {
  return useQuery({
    queryKey: accountSettingsQueryKeys.sessions,
    queryFn: getSessions,
  });
}
