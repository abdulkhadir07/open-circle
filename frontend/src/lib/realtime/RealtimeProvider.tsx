import { useEffect, type ReactNode } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { stompClient } from './stompClient';

type RealtimeErrorFrame = {
  status: number;
  error: string;
  message: string;
};

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const authStatus = useAuthStore((state) => state.authStatus);

  useEffect(() => {
    if (authStatus !== 'authenticated') return;

    stompClient.activate();

    const unsubscribeErrors = stompClient.subscribe('/user/queue/errors', (message) => {
      try {
        const frame = JSON.parse(message.body) as RealtimeErrorFrame;
        console.error('Realtime error', frame.status, frame.error, frame.message);
      } catch {
        console.error('Realtime error (unparseable)', message.body);
      }
    });

    return () => {
      unsubscribeErrors();
      stompClient.deactivate();
    };
  }, [authStatus]);

  return children;
}
