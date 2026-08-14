import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildNotificationAuthMessage,
  buildNotificationWebSocketUrl,
  createNotificationWebSocketConnection,
  notificationReconnectDelayMs,
} from './notifications';

class FakeWebSocket {
  readyState = WebSocket.CONNECTING;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  send = vi.fn();
  close = vi.fn(() => {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.({} as CloseEvent);
  });

  open() {
    this.readyState = WebSocket.OPEN;
    this.onopen?.({} as Event);
  }

  closeFromServer() {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.({} as CloseEvent);
  }
}

describe('notifications', () => {
  const originalWindow = globalThis.window;

  afterEach(() => {
    vi.useRealTimers();
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: originalWindow,
    });
  });

  it('builds a websocket url without access_token', () => {
    Object.defineProperty(globalThis, 'window', {
      configurable: true,
      value: {
        location: {
          origin: 'https://huashi.example',
        },
      },
    });

    const url = buildNotificationWebSocketUrl();

    expect(url).toBe('wss://huashi.example/ws/notifications');
    expect(url).not.toContain('access_token');
  });

  it('builds an AUTH first message', () => {
    expect(buildNotificationAuthMessage('jwt-token')).toBe(
      JSON.stringify({ type: 'AUTH', accessToken: 'jwt-token' })
    );
  });

  it('uses capped exponential reconnect delays', () => {
    expect(notificationReconnectDelayMs(0)).toBe(3_000);
    expect(notificationReconnectDelayMs(1)).toBe(6_000);
    expect(notificationReconnectDelayMs(3)).toBe(24_000);
    expect(notificationReconnectDelayMs(4)).toBe(30_000);
    expect(notificationReconnectDelayMs(50)).toBe(30_000);
  });

  it('authenticates after opening and backs off repeated disconnects', () => {
    vi.useFakeTimers();
    const sockets: FakeWebSocket[] = [];
    const cleanup = createNotificationWebSocketConnection({
      url: 'wss://huashi.example/ws/notifications',
      accessToken: 'jwt-token',
      onMessage: vi.fn(),
      socketFactory: () => {
        const socket = new FakeWebSocket();
        sockets.push(socket);
        return socket as unknown as WebSocket;
      },
    });

    vi.advanceTimersByTime(0);
    expect(sockets).toHaveLength(1);
    sockets[0].open();
    expect(sockets[0].send).toHaveBeenCalledWith(buildNotificationAuthMessage('jwt-token'));

    sockets[0].closeFromServer();
    vi.advanceTimersByTime(2_999);
    expect(sockets).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(2);

    sockets[1].closeFromServer();
    vi.advanceTimersByTime(5_999);
    expect(sockets).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(3);

    cleanup();
  });

  it('does not close a connecting socket during effect cleanup', () => {
    vi.useFakeTimers();
    const socket = new FakeWebSocket();
    const cleanup = createNotificationWebSocketConnection({
      url: 'wss://huashi.example/ws/notifications',
      accessToken: 'jwt-token',
      onMessage: vi.fn(),
      socketFactory: () => socket as unknown as WebSocket,
    });

    vi.advanceTimersByTime(0);
    cleanup();

    expect(socket.close).not.toHaveBeenCalled();
    socket.open();
    expect(socket.send).not.toHaveBeenCalled();
    expect(socket.close).toHaveBeenCalledOnce();
  });
});
