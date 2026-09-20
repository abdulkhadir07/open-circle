import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const constructedClients: MockClient[] = [];

class MockClient {
  connected = false;
  connectHeaders = {};
  onConnect: (() => void) | undefined;
  subscribe = vi.fn<(destination: string, callback: unknown) => { unsubscribe: () => void }>(
    () => ({
      unsubscribe: vi.fn<() => void>(),
    }),
  );
  activate = vi.fn<() => void>(() => {
    this.connected = true;
  });
  deactivate = vi.fn<() => Promise<void>>(() => Promise.resolve());

  constructor(config: { onConnect?: () => void }) {
    this.onConnect = config.onConnect;
    constructedClients.push(this);
  }
}

vi.mock('@stomp/stompjs', () => ({
  Client: MockClient,
}));

function latestClient(): MockClient {
  const client = constructedClients.at(-1);
  if (!client) throw new Error('No StompClientWrapper Client was constructed');
  return client;
}

describe('stompClient', () => {
  beforeEach(() => {
    constructedClients.length = 0;
    vi.resetModules();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('registers a subscription before activate() without touching the network', async () => {
    const { stompClient } = await import('./stompClient');

    const callback = vi.fn<() => void>();
    expect(() => stompClient.subscribe('/topic/chat-rooms/room-1', callback)).not.toThrow();
    expect(constructedClients).toHaveLength(0);
  });

  it('replays registered subscriptions once connected', async () => {
    const { stompClient } = await import('./stompClient');

    const callback = vi.fn<() => void>();
    stompClient.subscribe('/topic/chat-rooms/room-1', callback);
    stompClient.activate();

    const client = latestClient();
    client.onConnect?.();

    expect(client.subscribe).toHaveBeenCalledWith('/topic/chat-rooms/room-1', callback);
  });

  it('fires reconnect listeners on a later onConnect, but not the first one', async () => {
    const { stompClient } = await import('./stompClient');

    stompClient.activate();
    const client = latestClient();

    const reconnectListener = vi.fn<() => void>();
    stompClient.onReconnect(reconnectListener);

    client.onConnect?.();
    expect(reconnectListener).not.toHaveBeenCalled();

    client.onConnect?.();
    expect(reconnectListener).toHaveBeenCalledTimes(1);
  });

  it('deactivate() clears live subscriptions but keeps the registry for a later activate()', async () => {
    const { stompClient } = await import('./stompClient');

    const callback = vi.fn<() => void>();
    stompClient.subscribe('/topic/chat-rooms/room-1', callback);
    stompClient.activate();
    const firstClient = latestClient();
    firstClient.onConnect?.();

    stompClient.deactivate();
    expect(firstClient.deactivate).toHaveBeenCalled();

    stompClient.activate();
    const secondClient = latestClient();
    expect(secondClient).not.toBe(firstClient);
    secondClient.onConnect?.();

    expect(secondClient.subscribe).toHaveBeenCalledWith('/topic/chat-rooms/room-1', callback);
  });

  it('an unsubscribe function stops future delivery via the live subscription', async () => {
    const { stompClient } = await import('./stompClient');

    const callback = vi.fn<() => void>();
    stompClient.activate();
    const client = latestClient();
    client.onConnect?.();

    const unsubscribe = stompClient.subscribe('/topic/chat-rooms/room-1', callback);
    const liveSubscription = client.subscribe.mock.results.at(-1)?.value;

    unsubscribe();

    expect(liveSubscription.unsubscribe).toHaveBeenCalled();
  });
});
