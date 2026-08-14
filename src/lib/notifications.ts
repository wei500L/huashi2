const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';
const RECONNECT_BASE_DELAY_MS = 3_000;
const RECONNECT_MAX_DELAY_MS = 30_000;
const CONNECTION_STABLE_AFTER_MS = 30_000;

type NotificationWebSocketOptions = {
  url: string;
  accessToken: string;
  onMessage: (event: MessageEvent<string>) => void;
  socketFactory?: (url: string) => WebSocket;
};

export function buildNotificationWebSocketUrl(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const resolvedApiUrl = new URL(API_BASE_URL, window.location.origin);
  const wsProtocol = resolvedApiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  const normalizedPath = resolvedApiUrl.pathname.replace(/\/+$/, '');
  const wsPath = normalizedPath.endsWith('/api')
    ? `${normalizedPath.slice(0, -4) || ''}/ws/notifications`
    : `${normalizedPath}/ws/notifications`;

  const wsUrl = new URL(`${wsProtocol}//${resolvedApiUrl.host}${wsPath.startsWith('/') ? wsPath : `/${wsPath}`}`);
  return wsUrl.toString();
}

export function buildNotificationAuthMessage(accessToken: string): string {
  return JSON.stringify({ type: 'AUTH', accessToken });
}

export function notificationReconnectDelayMs(attempt: number): number {
  const normalizedAttempt = Number.isFinite(attempt) ? Math.max(0, Math.floor(attempt)) : 0;
  return Math.min(RECONNECT_BASE_DELAY_MS * (2 ** normalizedAttempt), RECONNECT_MAX_DELAY_MS);
}

export function createNotificationWebSocketConnection({
  url,
  accessToken,
  onMessage,
  socketFactory = (socketUrl) => new WebSocket(socketUrl),
}: NotificationWebSocketOptions): () => void {
  let disposed = false;
  let reconnectAttempt = 0;
  let reconnectTimer: number | null = null;
  let stableTimer: number | null = null;
  let socket: WebSocket | null = null;

  const clearStableTimer = () => {
    if (stableTimer !== null) {
      window.clearTimeout(stableTimer);
      stableTimer = null;
    }
  };

  const connect = () => {
    reconnectTimer = null;
    if (disposed) {
      return;
    }

    const nextSocket = socketFactory(url);
    socket = nextSocket;
    nextSocket.onopen = () => {
      if (disposed) {
        nextSocket.close();
        return;
      }
      nextSocket.send(buildNotificationAuthMessage(accessToken));
      clearStableTimer();
      stableTimer = window.setTimeout(() => {
        reconnectAttempt = 0;
        stableTimer = null;
      }, CONNECTION_STABLE_AFTER_MS);
    };
    nextSocket.onmessage = onMessage;
    nextSocket.onclose = () => {
      clearStableTimer();
      if (socket === nextSocket) {
        socket = null;
      }
      if (disposed) {
        return;
      }
      const delay = notificationReconnectDelayMs(reconnectAttempt);
      reconnectAttempt += 1;
      reconnectTimer = window.setTimeout(connect, delay);
    };
  };

  // Deferring the first connection lets React StrictMode finish its effect
  // probe without opening and immediately cancelling a WebSocket handshake.
  reconnectTimer = window.setTimeout(connect, 0);

  return () => {
    disposed = true;
    if (reconnectTimer !== null) {
      window.clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    clearStableTimer();
    if (socket?.readyState === WebSocket.OPEN) {
      socket.close();
    }
    // Calling close() while CONNECTING makes Chromium emit
    // "closed before the connection is established". If that handshake later
    // opens, the disposed onopen handler above closes it before authentication.
  };
}
