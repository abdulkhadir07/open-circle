import { useMutation } from '@tanstack/react-query';
import { resendVerification } from '../api/authApi';

export function useResendVerification() {
  return useMutation({ mutationFn: resendVerification });
}
