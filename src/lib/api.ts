import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import type { ApiResponse, LoginResponse } from './contracts';
import { clearStoredSession, dispatchAuthExpired, readStoredSession, writeStoredSession } from './session';

export class ApiError extends Error {
  status: number;
  code?: string;
  traceId?: string | null;

  constructor(message: string, status = 500, code?: string, traceId?: string | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
  }
}

const baseURL = import.meta.env.VITE_API_URL || '/api';
const http = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

const refreshClient = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let refreshPromise: Promise<LoginResponse | null> | null = null;

function withAuth(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
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
      !String(originalRequest.url || '').includes('/auth/login') &&
      !String(originalRequest.url || '').includes('/auth/refresh')
    ) {
      originalRequest._retry = true;
      const refreshedSession = await refreshSession();
      if (refreshedSession?.accessToken) {
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${refreshedSession.accessToken}`;
        return http(originalRequest);
      }
    }
    throw normalizeApiError(error);
  }
);

async function refreshSession(): Promise<LoginResponse | null> {
  const session = readStoredSession();
  if (!session?.refreshToken) {
    clearStoredSession();
    dispatchAuthExpired();
    return null;
  }
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken: session.refreshToken })
      .then((response) => {
        if (!response.data.success || !response.data.data) {
          throw new ApiError(response.data.message || 'Refresh failed', 401, response.data.code, response.data.traceId);
        }
        writeStoredSession(response.data.data);
        return response.data.data;
      })
      .catch((refreshError) => {
        clearStoredSession();
        dispatchAuthExpired();
        throw normalizeApiError(refreshError);
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
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

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.get<ApiResponse<T>>(url, config));
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.post<ApiResponse<T>>(url, data, config));
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.put<ApiResponse<T>>(url, data, config));
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap(await http.delete<ApiResponse<T>>(url, config));
}

export async function apiUpload<T>(url: string, formData: FormData): Promise<T> {
  return unwrap(
    await http.post<ApiResponse<T>>(url, formData, {
      headers: {
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
  return new Blob([response.data], {
    type: response.headers['content-type'] || 'application/octet-stream',
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
