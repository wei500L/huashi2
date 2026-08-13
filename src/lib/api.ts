import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import type { ApiResponse, LoginResponse, ResultCode } from './contracts';
import {
  clearStoredSession,
  consumeLegacyRefreshToken,
  dispatchAuthExpired,
  parseLoginResponse,
  readStoredSession,
  writeStoredSession,
} from './session';

export class ApiError extends Error {
  status: number;
  code?: ResultCode | string;
  traceId?: string | null;

  constructor(message: string, status = 500, code?: ResultCode | string, traceId?: string | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

const baseURL = import.meta.env.VITE_API_URL || '/api';
const AUTH_CSRF_HEADERS = {
  'Content-Type': 'application/json',
  'X-Requested-With': 'XMLHttpRequest',
} as const;

const http = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true,
  headers: AUTH_CSRF_HEADERS,
});

const refreshClient = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true,
  headers: AUTH_CSRF_HEADERS,
});

let refreshPromise: Promise<LoginResponse | null> | null = null;
const KEEPALIVE_TOKEN_FRESHNESS_WINDOW_MS = 15_000;
const ACCOUNT_SESSION_EXEMPT_PATHS = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/register/context',
  '/auth/refresh',
]);

function requestPath(url?: string): string {
  if (!url) {
    return '';
  }
  try {
    return new URL(url, 'http://localhost').pathname.replace(/^\/api(?=\/)/, '');
  } catch {
    return url.split('?')[0].replace(/^\/api(?=\/)/, '');
  }
}

function isAccountSessionExempt(url?: string): boolean {
  const path = requestPath(url);
  return ACCOUNT_SESSION_EXEMPT_PATHS.has(path)
    || path.startsWith('/auth/account-actions/')
    || path.startsWith('/public/assessments/');
}

function withAuth(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  if (isAccountSessionExempt(config.url)) {
    return config;
  }
  const session = readStoredSession();
  if (session?.accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return config;
}

http.interceptors.request.use(withAuth);

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAccountSessionExempt(originalRequest.url)
    ) {
      originalRequest._retry = true;
      const refreshedSession = await refreshSession().catch(() => null);
      if (refreshedSession?.accessToken) {
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${refreshedSession.accessToken}`;
        return http(originalRequest);
      }
    }
    throw normalizeApiError(error);
  }
);

async function refreshSession(
  legacyRefreshToken?: string | null,
  options?: { announceExpiry?: boolean; allowAnonymous?: boolean },
): Promise<LoginResponse | null> {
  const announceExpiry = options?.announceExpiry !== false;
  if (!refreshPromise) {
    const body = legacyRefreshToken ? { refreshToken: legacyRefreshToken } : {};
    refreshPromise = refreshClient
      .post<ApiResponse<LoginResponse | null>>('/auth/refresh', body)
      .then((response) => {
        if (!response.data.success) {
          throw new ApiError(response.data.message || 'Refresh failed', 401, response.data.code, response.data.traceId);
        }
        if (!response.data.data && options?.allowAnonymous) {
          clearStoredSession();
          return null;
        }
        const parsed = parseLoginResponse(response.data.data);
        if (!parsed) {
          throw new ApiError('Refresh returned an invalid session', 401);
        }
        writeStoredSession(parsed);
        return parsed;
      })
      .catch((refreshError) => {
        clearStoredSession();
        if (announceExpiry) {
          dispatchAuthExpired();
        }
        throw normalizeApiError(refreshError);
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

export async function restoreSessionFromCookie(): Promise<LoginResponse | null> {
  const legacyRefreshToken = consumeLegacyRefreshToken();
  try {
    return await refreshSession(legacyRefreshToken, { announceExpiry: false, allowAnonymous: true });
  } catch {
    return null;
  }
}

function isTokenExpiringSoon(expiresAt?: string | null, windowMs = KEEPALIVE_TOKEN_FRESHNESS_WINDOW_MS): boolean {
  if (!expiresAt) {
    return true;
  }
  const expiresAtMs = new Date(expiresAt).getTime();
  if (!Number.isFinite(expiresAtMs)) {
    return true;
  }
  return expiresAtMs - Date.now() <= windowMs;
}

function isTokenExpired(expiresAt?: string | null): boolean {
  return isTokenExpiringSoon(expiresAt, 0);
}

async function ensureFreshSessionForKeepalive(): Promise<LoginResponse | null> {
  const session = readStoredSession();
  if (!session?.accessToken) {
    return session;
  }
  if (!isTokenExpiringSoon(session.accessTokenExpiresAt)) {
    return session;
  }
  if (isTokenExpired(session.refreshTokenExpiresAt)) {
    return isTokenExpired(session.accessTokenExpiresAt) ? null : session;
  }
  try {
    return await refreshSession();
  } catch {
    if (!isTokenExpired(session.accessTokenExpiresAt)) {
      writeStoredSession(session);
      return session;
    }
    return readStoredSession();
  }
}

function unwrap<T>(response: AxiosResponse<ApiResponse<T>>): T {
  if (!response.data.success) {
    throw new ApiError(response.data.message || 'Request failed', response.status, response.data.code, response.data.traceId);
  }
  return response.data.data;
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    const payload = error.response?.data;
    return new ApiError(
      payload?.message || error.message || 'Request failed',
      error.response?.status || 500,
      payload?.code,
      payload?.traceId
    );
  }
  if (error instanceof Error) {
    return new ApiError(error.message);
  }
  return new ApiError('Unknown error');
}

const API_ERROR_MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: '用户名、邮箱或密码不正确。',
  CURRENT_PASSWORD_INCORRECT: '当前密码不正确。',
  ACCOUNT_LOCKED: '尝试次数过多，账号已被临时锁定，请稍后再试。',
  CONFLICT: '提交的信息与现有数据冲突，请检查后重试。',
  ACTIVE_SESSION_EXISTS: '已有进行中的会话，请先继续或放弃原会话。',
  ASSESSMENT_NOT_STARTED: '测评尚未开始，请在开始时间后重试。',
  ASSESSMENT_CLOSED: '测评已截止，无法再开始作答。',
  ATTEMPT_SUBMITTED: '答卷已经提交，不能继续修改。',
  RESULT_NOT_RELEASED: '答卷已提交，结果尚未公布。',
  VERSION_CONFLICT: '答卷已在其他页面或设备更新，请刷新后再继续。',
  SESSION_OUT_OF_SEQUENCE: '题目顺序已变化，正在同步服务器当前题。',
  ANSWER_ALREADY_ACCEPTED: '本题答案已经提交，正在同步下一题。',
  PARTICIPATION_CODE_INVALID: '参与码无效或已失效，请检查后重试。',
  REGISTRATION_CONTEXT_INVALID: '邀请码验证已失效，请重新输入并验证邀请码。',
  REGISTRATION_CONTEXT_BUSY: '当前注册正在处理中，请稍后再试。',
  RATE_LIMITED: '请求过于频繁，请稍后再试。',
  VALIDATION_ERROR: '提交内容未通过校验，请检查后重试。',
  AI_PROVIDER_UNAVAILABLE: 'AI 服务暂时不可用，请稍后再试。',
  TOKEN_EXPIRED: '登录状态已过期，请重新登录。',
};

export function getApiErrorMessage(error: unknown, fallback = '请求失败'): string {
  const normalizedError = normalizeApiError(error);
  const validationMessage = researchValidationMessage(normalizedError);
  if (validationMessage) {
    return validationMessage;
  }
  if (normalizedError.code && API_ERROR_MESSAGES[normalizedError.code]) {
    return API_ERROR_MESSAGES[normalizedError.code];
  }
  return normalizedError.message || fallback;
}

function researchValidationMessage(error: ReturnType<typeof normalizeApiError>): string | null {
  if (error.code !== 'VALIDATION_ERROR') {
    return null;
  }
  const message = error.message || '';
  const required = /Required question (\d+)/i.exec(message);
  if (required) {
    return `第 ${required[1]} 题尚未作答，请完成后再提交。`;
  }
  if (/justification is required/i.test(message)) {
    return '判断为错误时需要填写理由。';
  }
  return null;
}

function resolveRequestUrl(url: string): string {
  if (typeof window === 'undefined') {
    return url;
  }
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  const resolvedBaseUrl = new URL(baseURL.endsWith('/') ? baseURL : `${baseURL}/`, window.location.origin);
  return new URL(url.startsWith('/') ? url.slice(1) : url, resolvedBaseUrl).toString();
}

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T> | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch {
    return null;
  }
}

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.get<ApiResponse<T>>(url, config));
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.post<ApiResponse<T>>(url, data, config));
}

export async function apiPostKeepalive<T>(url: string, data?: unknown): Promise<T> {
  const session = await ensureFreshSessionForKeepalive();
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 8000);
  let response: Response;
  try {
    response = await fetch(resolveRequestUrl(url), {
      method: 'POST',
      keepalive: true,
      credentials: 'include',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        ...(session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {}),
      },
      body: JSON.stringify(data ?? {}),
    });
  } finally {
    window.clearTimeout(timeout);
  }
  const payload = await parseApiResponse<T>(response);
  if (response.status === 401 || payload?.code === 'TOKEN_EXPIRED') {
    dispatchAuthExpired();
  }
  if (!response.ok) {
    throw new ApiError(
      payload?.message || response.statusText || 'Request failed',
      response.status,
      payload?.code,
      payload?.traceId
    );
  }
  if (!payload?.success) {
    throw new ApiError(payload?.message || 'Request failed', response.status, payload?.code, payload?.traceId);
  }
  return payload.data;
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.put<ApiResponse<T>>(url, data, config));
}

export async function apiPatch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.patch<ApiResponse<T>>(url, data, config));
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.delete<ApiResponse<T>>(url, config));
}

export async function apiUpload<T>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(
    await http.post<ApiResponse<T>>(url, formData, {
      ...config,
      headers: {
        ...(config?.headers || {}),
        'Content-Type': 'multipart/form-data',
      },
    })
  );
}

export async function apiDownload(url: string, config?: AxiosRequestConfig): Promise<Blob> {
  const response = await http.get<ArrayBuffer>(url, {
    ...config,
    responseType: 'arraybuffer',
  });
  const contentType = response.headers['content-type'];
  return new Blob([response.data], {
    type: typeof contentType === 'string' ? contentType : 'application/octet-stream',
  });
}

export function saveBlob(blob: Blob, filename: string): void {
  const objectUrl = window.URL.createObjectURL(blob);
  const anchor = window.document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = filename;
  window.document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(objectUrl);
}
