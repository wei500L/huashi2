import type { LoginResponse } from './contracts';

export const SESSION_STORAGE_KEY = 'ef-transfer-session';
export const SESSION_CHANGE_EVENT = 'ef-transfer-session-change';

export function readStoredSession(): LoginResponse | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as LoginResponse;
  } catch {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function writeStoredSession(session: LoginResponse): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
}

export function clearStoredSession(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
}
