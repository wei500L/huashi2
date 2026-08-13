import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AxiosAdapter, AxiosResponse } from 'axios';
import type { ApiResponse, LoginResponse } from './contracts';
import { ApiError, apiGet, apiPost, apiPostKeepalive, getApiErrorMessage, restoreSessionFromCookie } from './api';
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

function successfulAdapter<T>(onRequest: AxiosAdapter): AxiosAdapter {
  return async (config) => {
    await onRequest(config);
    return {
      data: { success: true, data: {} as T },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    } as AxiosResponse<ApiResponse<T>>;
  };
}

describe('account-session isolation', () => {
  beforeEach(() => {
    window.localStorage.clear();
    clearStoredSession();
    writeStoredSession(mockSession);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    clearStoredSession();
  });

  it('does not attach the account bearer token to public questionnaire requests', async () => {
    const inspectRequest = vi.fn();

    await apiGet('/public/assessments/RES-TEST', {
      adapter: successfulAdapter(inspectRequest),
    });

    expect(inspectRequest).toHaveBeenCalledWith(
      expect.objectContaining({ headers: expect.not.objectContaining({ Authorization: expect.anything() }) })
    );
  });

  it('does not attach a stale account bearer token to login requests', async () => {
    const inspectRequest = vi.fn();

    await apiPost('/auth/login', { usernameOrEmail: 'student', password: 'secret' }, {
      adapter: successfulAdapter(inspectRequest),
    });

    expect(inspectRequest).toHaveBeenCalledWith(
      expect.objectContaining({ headers: expect.not.objectContaining({ Authorization: expect.anything() }) })
    );
  });
});

describe('public questionnaire errors', () => {
  it('maps an invalid participation code to a stable Chinese message', () => {
    expect(getApiErrorMessage(new ApiError(
      'Participation code is invalid or unavailable',
      422,
      'PARTICIPATION_CODE_INVALID'
    ))).toBe('参与码无效或已失效，请检查后重试。');
  });

  it('surfaces the unanswered required question number', () => {
    expect(getApiErrorMessage(new ApiError(
      'Required question 12 has not been answered',
      400,
      'VALIDATION_ERROR'
    ))).toBe('第 12 题尚未作答，请完成后再提交。');
  });

  it('surfaces a missing true/false justification', () => {
    expect(getApiErrorMessage(new ApiError(
      'A justification is required when a true/false answer is F',
      400,
      'VALIDATION_ERROR'
    ))).toBe('判断为错误时需要填写理由。');
  });
});

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

  it('does not treat a failed cookie restore as an expired login', async () => {
    const result = await restoreSessionFromCookie();
    expect(result).toBeNull();
    expect(hasPendingAuthExpired()).toBe(false);
    expect(readStoredSession()).toBeNull();
  });
});
