const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

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
