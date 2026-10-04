import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createBanter } from '../api/banterApi';
import { banterQueryKeys } from '../api/queryKeys';

export function useCreateBanter() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createBanter,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: banterQueryKeys.lists });
    },
  });
}
