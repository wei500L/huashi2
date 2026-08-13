import { z } from 'zod';
import type { LoginResponse } from './contracts';
import { UserCapabilityValues, UserRoleValues } from './contracts/generated/session-domain';

export const SESSION_STORAGE_KEY = 'ef-transfer-session';
export const SESSION_CHANGE_EVENT = 'ef-transfer-session-change';
export const AUTH_EXPIRED_EVENT = 'ef-transfer-auth-expired';

const currentUserSchema = z.object({
  id: z.number(),
  username: z.string().min(1),
  email: z.string().min(1),
  displayName: z.string().min(1),
  lastLoginAt: z.string().nullable().optional(),
  primaryRole: z.enum(UserRoleValues),
  roles: z.array(z.enum(UserRoleValues)),
  capabilities: z.array(z.enum(UserCapabilityValues)),
  studentProfile: z.unknown().nullable().optional(),
  teacherProfile: z.unknown().nullable().optional(),
});

const loginResponseSchema = z.object({
  accessToken: z.string().min(1),
  accessTokenExpiresAt: z.string().min(1),
  refreshTokenExpiresAt: z.string().min(1),
  userInfo: currentUserSchema,
});

let memorySession: LoginResponse | null = null;
let authExpiredPending = false;

export function parseLoginResponse(value: unknown): LoginResponse | null {
  const parsed = loginResponseSchema.safeParse(value);
  if (!parsed.success) {
    return null;
  }
  return parsed.data as LoginResponse;
}

export function readStoredSession(): LoginResponse | null {
  return memorySession;
}

export function writeStoredSession(session: LoginResponse): void {
  const parsed = parseLoginResponse(session);
  if (!parsed) {
    clearStoredSession();
    return;
  }
  memorySession = parsed;
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
  }
}

export function clearStoredSession(): void {
  memorySession = null;
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
  }
}

export function consumeLegacyRefreshToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as { refreshToken?: unknown };
    return typeof parsed.refreshToken === 'string' && parsed.refreshToken.trim()
      ? parsed.refreshToken.trim()
      : null;
  } catch {
    return null;
  }
}

export function dispatchAuthExpired(): void {
  if (typeof window === 'undefined') {
    return;
  }
  authExpiredPending = true;
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
}

export function hasPendingAuthExpired(): boolean {
  return authExpiredPending;
}

export function clearPendingAuthExpired(): void {
  authExpiredPending = false;
}
