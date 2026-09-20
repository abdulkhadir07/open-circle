import { Client, type IMessage } from '@stomp/stompjs';
import { env } from '@/lib/env';
import { useAuthStore } from '@/stores/authStore';

function resolveBrokerUrl(): string {
  if (env.wsUrl) return env.wsUrl;

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

type Subscriber = { destination: string; callback: (message: IMessage) => void };

/**
 * A single WebSocket/STOMP connection shared by the whole app, matching how
 * apiClient/queryClient are already plain singletons here. subscribe() works
 * before activate() (and stays registered across a deactivate()) — it just
 * records the intent and starts delivering once a connection exists, so
 * callers never need to know or wait for connection state.
 */
class StompClientWrapper {
  private client: Client | null = null;
  private subscribers = new Map<number, Subscriber>();
  private live = new Map<number, { unsubscribe: () => void }>();
  private nextId = 0;
  private hasConnectedBefore = false;
  private reconnectListeners = new Set<() => void>();

  activate(): void {
    if (this.client) return;

    const client = new Client({
      brokerURL: resolveBrokerUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: (activeClient) => {
        const token = useAuthStore.getState().accessToken;
        activeClient.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
      onConnect: () => {
        const isReconnect = this.hasConnectedBefore;
        this.hasConnectedBefore = true;
        this.resubscribeAll();
        if (isReconnect) {
          this.reconnectListeners.forEach((listener) => listener());
        }
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame.headers, frame.body);
      },
      onWebSocketError: (event) => {
        console.error('WebSocket error', event);
      },
    });

    this.client = client;
    client.activate();
  }

  deactivate(): void {
    void this.client?.deactivate();
    this.client = null;
    this.live.clear();
    this.hasConnectedBefore = false;
    // The subscriber registry is intentionally kept — a later activate()
    // (e.g. re-login) replays whatever callers are still subscribed to.
  }

  subscribe(destination: string, callback: (message: IMessage) => void): () => void {
    const id = this.nextId++;
    this.subscribers.set(id, { destination, callback });

    if (this.client?.connected) {
      this.live.set(id, this.client.subscribe(destination, callback));
    }

    return () => {
      this.subscribers.delete(id);
      this.live.get(id)?.unsubscribe();
      this.live.delete(id);
    };
  }

  onReconnect(listener: () => void): () => void {
    this.reconnectListeners.add(listener);
    return () => this.reconnectListeners.delete(listener);
  }

  private resubscribeAll(): void {
    this.live.clear();
    if (!this.client) return;

    for (const [id, { destination, callback }] of this.subscribers) {
      this.live.set(id, this.client.subscribe(destination, callback));
    }
  }
}

export const stompClient = new StompClientWrapper();
