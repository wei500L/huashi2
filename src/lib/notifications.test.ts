import { afterEach, describe, expect, it } from 'vitest';
import { buildNotificationAuthMessage, buildNotificationWebSocketUrl } from './notifications';

describe('notifications', () => {
  const originalWindow = globalThis.window;

  afterEach(() => {
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
});
