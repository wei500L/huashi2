import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Outlet, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react';
import type { CurrentUserVO, LoginResponse } from '@/lib/contracts';
import { clearStoredSession, dispatchAuthExpired, hasPendingAuthExpired } from '@/lib/session';
import { workspacePreferenceKey } from '@/lib/workspaces';
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

vi.mock('./pages/teacher/Workspace', () => ({
  default: () => <div>teacher-workspace</div>,
}));

vi.mock('./pages/admin/index', () => ({
  default: () => <div>admin-users</div>,
}));

vi.mock('./pages/admin/Dashboard', () => ({
  default: () => <div>admin-dashboard</div>,
}));

vi.mock('./pages/Login', () => ({
  default: function LoginMock() {
    const location = useLocation();
    const state = location.state as { expired?: boolean; from?: string; passwordChanged?: boolean } | null;
    const passwordChanged = Boolean(state?.passwordChanged);
    const expired = !passwordChanged && (Boolean(state?.expired) || hasPendingAuthExpired());

    return (
      <div data-testid="login-page">
        {expired ? 'expired' : 'fresh'}:{passwordChanged ? 'changed' : 'plain'}:{state?.from ?? 'none'}
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

const multiWorkspaceUser: CurrentUserVO = {
  ...mockUser,
  primaryRole: 'ADMIN',
  roles: ['ADMIN', 'TEACHER', 'STUDENT'],
  capabilities: ['ADMIN_CONSOLE', 'TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'],
};

const multiWorkspaceSession: LoginResponse = {
  ...mockSession,
  userInfo: multiWorkspaceUser,
};

const teacherUser: CurrentUserVO = {
  ...mockUser,
  primaryRole: 'TEACHER',
  roles: ['TEACHER'],
  capabilities: ['TEACHING_WORKSPACE'],
};

const teacherSession: LoginResponse = {
  ...mockSession,
  userInfo: teacherUser,
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
      activeWorkspace: null,
      preferredWorkspaceByUser: {},
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
      expect(screen.getByTestId('login-page')).toHaveTextContent('expired:plain:/dashboard');
    });
  });

  it('keeps the password-changed login state when auth-expired fires afterwards', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'anonymous',
      session: null,
      user: null,
      error: null,
      initialize: vi.fn().mockResolvedValue(undefined),
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[{ pathname: '/login', state: { passwordChanged: true } }]}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByTestId('login-page')).toHaveTextContent('fresh:changed:none');

    await act(async () => {
      dispatchAuthExpired();
    });

    await waitFor(() => {
      expect(screen.getByTestId('login-page')).toHaveTextContent('fresh:changed:none');
    });
  });

  it('redirects root to the remembered workspace home for multi-workspace users', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const preferenceKey = workspacePreferenceKey(multiWorkspaceUser);

    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: multiWorkspaceSession,
      user: multiWorkspaceUser,
      error: null,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: null,
      preferredWorkspaceByUser: preferenceKey ? { [preferenceKey]: 'TEACHING_WORKSPACE' } : {},
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByText('teacher-workspace')).toBeInTheDocument();
    expect(useUIStore.getState().activeWorkspace).toBe('TEACHING_WORKSPACE');
  });

  it('syncs the active workspace from a direct deep link', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: multiWorkspaceSession,
      user: multiWorkspaceUser,
      error: null,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: 'ADMIN_CONSOLE',
      preferredWorkspaceByUser: {},
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/teacher/workspace']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByText('teacher-workspace')).toBeInTheDocument();
    expect(useUIStore.getState().activeWorkspace).toBe('TEACHING_WORKSPACE');
  });

  it('falls back to the current workspace home when access is denied', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: teacherSession,
      user: teacherUser,
      error: null,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: 'TEACHING_WORKSPACE',
      preferredWorkspaceByUser: {},
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/admin/users']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByText('teacher-workspace')).toBeInTheDocument();
    expect(useUIStore.getState().activeWorkspace).toBe('TEACHING_WORKSPACE');
  });
});
