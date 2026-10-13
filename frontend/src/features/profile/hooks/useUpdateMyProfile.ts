import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { AuthUser } from '@/features/auth/api/contracts';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { updateMyProfile, type UpdateProfilePayload } from '../api/profileApi';
import { profileQueryKeys } from '../api/queryKeys';

export function useUpdateMyProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateMyProfile(payload),
    onSuccess: (profile) => {
      queryClient.setQueryData(profileQueryKeys.detail(profile.userId), profile);
      queryClient.setQueryData<AuthUser>(authQueryKeys.currentUser, (currentUser) =>
        currentUser ? { ...currentUser, username: profile.username } : currentUser,
      );
    },
  });
}
