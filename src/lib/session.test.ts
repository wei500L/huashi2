import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { LoginResponse } from './contracts';
import {
  SESSION_STORAGE_KEY,
  clearStoredSession,
  consumeLegacyRefreshToken,
  parseLoginResponse,
  readStoredSession,
  writeStoredSession,
} from './session';

const validSession: LoginResponse = {
  accessToken: 'access-token',
  accessTokenExpiresAt: '2030-01-01T00:00:00.000Z',
  refreshTokenExpiresAt: '2030-01-08T00:00:00.000Z',
  userInfo: {
    id: 1,
    username: 'student.demo',
    email: 'student@example.com',
    displayName: 'Student Demo',
    primaryRole: 'STUDENT',
    roles: ['STUDENT'],
    capabilities: ['STUDENT_WORKSPACE'],
    studentProfile: null,
    teacherProfile: null,
  },
};

describe('session boundary', () => {
  beforeEach(() => {
    window.localStorage.clear();
    clearStoredSession();
  });

  afterEach(() => {
    clearStoredSession();
    window.localStorage.clear();
  });

  it('keeps access tokens in memory and never writes them to localStorage', () => {
    writeStoredSession(validSession);
    expect(readStoredSession()?.accessToken).toBe('access-token');
    expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('rejects incomplete login payloads and clears the session', () => {
    writeStoredSession(validSession);
    writeStoredSession({ accessToken: 'stale' } as LoginResponse);
    expect(readStoredSession()).toBeNull();
  });

  it('consumes a one-time legacy refresh token then removes localStorage', () => {
    window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({
      ...validSession,
      refreshToken: 'legacy-refresh',
    }));
    expect(consumeLegacyRefreshToken()).toBe('legacy-refresh');
    expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    expect(consumeLegacyRefreshToken()).toBeNull();
  });

  it('parseLoginResponse ignores extra refreshToken fields', () => {
    expect(parseLoginResponse({
      ...validSession,
      refreshToken: 'should-not-be-required',
    })?.accessToken).toBe('access-token');
    expect(parseLoginResponse({
      accessToken: 'x',
      accessTokenExpiresAt: '2030-01-01T00:00:00.000Z',
    })).toBeNull();
  });
});
