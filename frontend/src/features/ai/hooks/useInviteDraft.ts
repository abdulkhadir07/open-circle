import { useMutation } from '@tanstack/react-query';
import { draftInvite } from '../api/aiApi';

export function useInviteDraft() {
  return useMutation({ mutationFn: draftInvite });
}
