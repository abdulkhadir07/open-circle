import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateMyProfile, type UpdateProfilePayload } from '../api/profileApi';
import { profileQueryKeys } from '../api/queryKeys';

export function useUpdateMyProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateMyProfile(payload),
    onSuccess: (profile) => {
      queryClient.setQueryData(profileQueryKeys.detail(profile.userId), profile);
    },
  });
}
