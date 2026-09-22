import { useEffect, type ReactNode } from 'react';
import { useNotificationsRealtime } from './hooks/useNotificationsRealtime';
import { unlockAudioContext } from './lib/playNotificationSound';

/**
 * Mounted app-wide (not just on HomePage) so the bell's unread count and
 * cached inbox stay current no matter what page the user is on — only the
 * bell/dropdown UI itself is HomePage-only.
 */
export function NotificationsRealtimeProvider({ children }: { children: ReactNode }) {
  useNotificationsRealtime();

  useEffect(() => {
    // Primes the shared AudioContext on the first interaction anywhere in the
    // app, so a notification sound isn't silently dropped by the browser's
    // autoplay policy if it arrives before the user has otherwise clicked or
    // typed something.
    function unlock() {
      unlockAudioContext();
      document.removeEventListener('pointerdown', unlock);
      document.removeEventListener('keydown', unlock);
    }

    document.addEventListener('pointerdown', unlock);
    document.addEventListener('keydown', unlock);

    return () => {
      document.removeEventListener('pointerdown', unlock);
      document.removeEventListener('keydown', unlock);
    };
  }, []);

  return children;
}
