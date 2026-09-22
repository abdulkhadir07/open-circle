import { useMutation, useQueryClient } from '@tanstack/react-query';
import { submitRating } from '../api/ratingsApi';
import { ratingQueryKeys } from '../api/queryKeys';

export function useSubmitRating(engagementId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (score: number) => submitRating(engagementId, score),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ratingQueryKeys.due });
    },
  });
}
