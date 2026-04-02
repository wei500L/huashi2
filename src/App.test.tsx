import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Outlet, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react';
import type { CurrentUserVO, LoginResponse } from '@/lib/contracts';
import { clearStoredSession, dispatchAuthExpired, hasPendingAuthExpired } from '@/lib/session';
import { useAuthStore, useUIStore } from '@/store';
import App from './App';

vi.mock('./components/layout', () => ({
  AppLayout: () => (
    <div data-testid="layout-shell">
      <Outlet />
    </div>
  ),
}));

vi.mock('./pages/dashboard/index', () => ({
  default: () => <div>dashboard</div>,
}));

vi.mock('./pages/Login', () => ({
  default: function LoginMock() {
    const location = useLocation();
    const state = location.state as { expired?: boolean; from?: string } | null;
    const expired = Boolean(state?.expired) || hasPendingAuthExpired();

    return (
      <div data-testid="login-page">
        {expired ? 'expired' : 'fresh'}:{state?.from ?? 'none'}
      </div>
    );
  },
}));

const mockUser: CurrentUserVO = {
  id: 1,
  username: 'student.demo',
  email: 'student@example.com',
  displayName: 'Student Demo',
  primaryRole: 'STUDENT',
  roles: ['STUDENT'],
  capabilities: ['STUDENT_WORKSPACE'],
  studentProfile: null,
  teacherProfile: null,
};

const mockSession: LoginResponse = {
  accessToken: 'access-token',
  accessTokenExpiresAt: '2030-01-01T00:00:00Z',
  refreshToken: 'refresh-token',
  refreshTokenExpiresAt: '2030-01-02T00:00:00Z',
  userInfo: mockUser,
};

const originalAuthState = useAuthStore.getState();
const originalUiState = useUIStore.getState();

describe('App auth-expired handling', () => {
  beforeEach(() => {
    window.localStorage.clear();
    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: mockSession,
      user: mockUser,
      error: null,
      initialize: vi.fn().mockResolvedValue(undefined),
      syncFromStorage: originalAuthState.syncFromStorage,
      isAuthenticated: () => true,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isDarkMode: false,
    });
  });

  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('redirects back to login with the original path when auth expires', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/dashboard']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByText('dashboard')).toBeInTheDocument();

    await act(async () => {
      clearStoredSession();
      dispatchAuthExpired();
    });

    await waitFor(() => {
      expect(screen.getByTestId('login-page')).toHaveTextContent('expired:/dashboard');
    });
  });
});
