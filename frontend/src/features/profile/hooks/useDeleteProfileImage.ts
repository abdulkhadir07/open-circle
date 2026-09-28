import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { AuthUser } from '@/features/auth/api/contracts';
import { authQueryKeys } from '@/features/auth/api/queryKeys';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import type { UserProfile } from '../api/contracts';
import { deleteProfileImage } from '../api/profileApi';
import { profileQueryKeys } from '../api/queryKeys';

export function useDeleteProfileImage() {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();

  return useMutation({
    mutationFn: deleteProfileImage,
    onSuccess: () => {
      queryClient.setQueryData<AuthUser>(authQueryKeys.currentUser, (old) =>
        old ? { ...old, profileImage: null } : old,
      );
      if (currentUser.data) {
        queryClient.setQueryData<UserProfile>(
          profileQueryKeys.detail(currentUser.data.id),
          (old) => (old ? { ...old, profileImage: null } : old),
        );
      }
    },
  });
}
