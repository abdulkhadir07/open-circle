import { useQuery } from '@tanstack/react-query';
import { getBanterReplies } from '../api/banterApi';
import { banterQueryKeys } from '../api/queryKeys';

export function useBanterReplies(banterId: string, enabled: boolean) {
  return useQuery({
    queryKey: banterQueryKeys.replies(banterId),
    queryFn: () => getBanterReplies(banterId),
    enabled,
  });
}
