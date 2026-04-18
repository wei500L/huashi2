import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { LoginResponse } from './contracts';
import { apiPostKeepalive } from './api';
import {
  clearPendingAuthExpired,
  clearStoredSession,
  hasPendingAuthExpired,
  readStoredSession,
  writeStoredSession,
} from './session';

const mockSession: LoginResponse = {
  accessToken: 'access-token',
  accessTokenExpiresAt: '2030-01-01T00:00:10.000Z',
  refreshToken: 'refresh-token',
  refreshTokenExpiresAt: '2030-01-01T00:00:00.000Z',
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

describe('apiPostKeepalive', () => {
  beforeEach(() => {
    vi.spyOn(Date, 'now').mockReturnValue(new Date('2030-01-01T00:00:00.000Z').getTime());
    window.localStorage.clear();
    clearPendingAuthExpired();
    clearStoredSession();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    clearPendingAuthExpired();
    clearStoredSession();
  });

  it('falls back to the still-valid access token when the refresh token is already expired', async () => {
    writeStoredSession(mockSession);
    const fetchSpy = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(
        new Response(JSON.stringify({ success: true, data: { ok: true } }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      );

    const result = await apiPostKeepalive<{ ok: boolean }>('/student/assessments/attempts/42/responses', { foo: 'bar' });

    expect(result).toEqual({ ok: true });
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.stringContaining('/student/assessments/attempts/42/responses'),
      expect.objectContaining({
        keepalive: true,
        headers: expect.objectContaining({
          Authorization: 'Bearer access-token',
        }),
      })
    );
    expect(readStoredSession()?.accessToken).toBe('access-token');
    expect(hasPendingAuthExpired()).toBe(false);
  });
});
