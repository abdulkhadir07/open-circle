import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { verifyLocation } from '../api/locationApi';

export function useVerifyLocation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: verifyLocation,
    onSuccess: (user) => {
      queryClient.setQueryData(authQueryKeys.currentUser, user);
    },
  });
}
