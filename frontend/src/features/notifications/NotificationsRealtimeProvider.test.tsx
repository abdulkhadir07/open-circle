import { render } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationsRealtimeProvider } from './NotificationsRealtimeProvider';

const { useNotificationsRealtimeMock, unlockAudioContextMock } = vi.hoisted(() => ({
  useNotificationsRealtimeMock: vi.fn<() => void>(),
  unlockAudioContextMock: vi.fn<() => void>(),
}));

vi.mock('./hooks/useNotificationsRealtime', () => ({
  useNotificationsRealtime: useNotificationsRealtimeMock,
}));

vi.mock('./lib/playNotificationSound', () => ({
  unlockAudioContext: unlockAudioContextMock,
}));

describe('NotificationsRealtimeProvider', () => {
  beforeEach(() => {
    useNotificationsRealtimeMock.mockReset();
    unlockAudioContextMock.mockReset();
  });

  it('mounts the realtime subscription', () => {
    render(<NotificationsRealtimeProvider>content</NotificationsRealtimeProvider>);

    expect(useNotificationsRealtimeMock).toHaveBeenCalled();
  });

  it('unlocks audio on the first pointerdown anywhere on the page', () => {
    render(<NotificationsRealtimeProvider>content</NotificationsRealtimeProvider>);

    document.dispatchEvent(new Event('pointerdown'));

    expect(unlockAudioContextMock).toHaveBeenCalledTimes(1);
  });

  it('only unlocks once even if further gestures happen', () => {
    render(<NotificationsRealtimeProvider>content</NotificationsRealtimeProvider>);

    document.dispatchEvent(new Event('pointerdown'));
    document.dispatchEvent(new Event('pointerdown'));
    document.dispatchEvent(new Event('keydown'));

    expect(unlockAudioContextMock).toHaveBeenCalledTimes(1);
  });

  it('unlocks on keydown when pointerdown never fires', () => {
    render(<NotificationsRealtimeProvider>content</NotificationsRealtimeProvider>);

    document.dispatchEvent(new Event('keydown'));

    expect(unlockAudioContextMock).toHaveBeenCalledTimes(1);
  });

  it('removes listeners on unmount', () => {
    const { unmount } = render(
      <NotificationsRealtimeProvider>content</NotificationsRealtimeProvider>,
    );

    unmount();
    document.dispatchEvent(new Event('pointerdown'));
    document.dispatchEvent(new Event('keydown'));

    expect(unlockAudioContextMock).not.toHaveBeenCalled();
  });
});
