import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { AuthUser } from '@/features/auth/api/contracts';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import type { UserProfile } from '../api/contracts';
import { uploadProfileImage } from '../api/profileApi';
import { profileQueryKeys } from '../api/queryKeys';

export function useUploadProfileImage() {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();

  return useMutation({
    mutationFn: uploadProfileImage,
    onSuccess: (profileImage) => {
      queryClient.setQueryData<AuthUser>(authQueryKeys.currentUser, (old) =>
        old ? { ...old, profileImage } : old,
      );
      if (currentUser.data) {
        queryClient.setQueryData<UserProfile>(
          profileQueryKeys.detail(currentUser.data.id),
          (old) => (old ? { ...old, profileImage } : old),
        );
      }
    },
  });
}
