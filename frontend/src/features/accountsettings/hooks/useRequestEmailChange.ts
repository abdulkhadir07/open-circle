import { useMutation } from '@tanstack/react-query';
import { requestEmailChange, type RequestEmailChangePayload } from '../api/accountSettingsApi';

export function useRequestEmailChange() {
  return useMutation({
    mutationFn: (payload: RequestEmailChangePayload) => requestEmailChange(payload),
  });
}
