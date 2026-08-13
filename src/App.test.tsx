import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Outlet, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen, waitFor } from '@testing-library/react';
import type { CurrentUserVO, LoginResponse } from '@/lib/contracts';
import {
  clearPendingAuthExpired,
  clearStoredSession,
  dispatchAuthExpired,
  hasPendingAuthExpired,
  writeStoredSession,
} from '@/lib/session';
import { workspacePreferenceKey } from '@/lib/workspaces';
import { authService } from '@/lib/services';
import { useAuthStore, useUIStore } from '@/store';
import App from './App';

const initializeMock = vi.hoisted(() => vi.fn().mockResolvedValue(undefined));

vi.mock('@/store', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/store')>();
  actual.useAuthStore.setState({ initialize: initializeMock });
  return actual;
});

vi.mock('@/lib/services', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/services')>();
  return {
    ...actual,
    authService: {
      ...actual.authService,
      me: vi.fn(),
      logout: vi.fn().mockResolvedValue(undefined),
    },
  };
});

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

vi.mock('./pages/student/Settings', () => ({
  default: () => <div>settings-page</div>,
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

vi.mock('./pages/research/index', () => ({
  default: () => <div data-testid="research-page">research-page</div>,
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
  refreshTokenExpiresAt: '2030-01-02T00:00:00Z',
  userInfo: mockUser,
};

const completeStudentProfile = {
  studentNo: 'S20260099',
  gradeName: '高一（3）班',
  frenchLevel: 'A2',
  courseStage: 'INTERMEDIATE',
  compositeScore: 0,
  dailyTrainingTarget: null,
  weeklyAccuracyTarget: null,
} as const;

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

describe('App public research auth isolation', () => {
  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders the public questionnaire without initializing the account session', async () => {
    const initialize = vi.fn().mockResolvedValue(undefined);
    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'idle',
      session: mockSession,
      user: mockUser,
      error: null,
      initialize,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isDarkMode: false,
    });

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={['/research/RES-AFC02D0823F2']}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByTestId('research-page')).toBeInTheDocument();
    expect(initialize).not.toHaveBeenCalled();
  });
});

function setAuthenticatedSession(session: LoginResponse) {
  vi.mocked(authService.me).mockResolvedValue(session.userInfo);
  writeStoredSession(session);
  useAuthStore.setState({
    ...useAuthStore.getState(),
    status: 'authenticated',
    session,
    user: session.userInfo,
    error: null,
    initialize: initializeMock,
    syncFromStorage: originalAuthState.syncFromStorage,
    isAuthenticated: () => true,
  });
}

describe('App auth-expired handling', () => {
  beforeEach(() => {
    window.localStorage.clear();
    clearPendingAuthExpired();
    setAuthenticatedSession(mockSession);
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
    clearPendingAuthExpired();
    clearStoredSession();
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
      initialize: initializeMock,
    });
    clearStoredSession();
    clearStoredSession();

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
    const session = {
      ...multiWorkspaceSession,
      userInfo: {
        ...multiWorkspaceUser,
        studentProfile: completeStudentProfile,
      },
    };
    setAuthenticatedSession(session);
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

    setAuthenticatedSession(multiWorkspaceSession);
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

    setAuthenticatedSession(teacherSession);
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

  it('routes authenticated students with incomplete profiles to settings from login', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    setAuthenticatedSession({
      ...mockSession,
      userInfo: {
        ...mockUser,
        studentProfile: {
          studentNo: 'S20260001',
          gradeName: '',
          frenchLevel: 'A1',
          courseStage: 'FOUNDATION',
          compositeScore: 0,
          dailyTrainingTarget: null,
          weeklyAccuracyTarget: null,
        },
      },
    });

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/login']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    expect(await screen.findByText('settings-page')).toBeInTheDocument();
  });

  it('keeps multi-workspace users on their default workspace home when only the student profile is incomplete', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    setAuthenticatedSession(multiWorkspaceSession);

    await act(async () => {
      render(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/']}>
            <App />
          </MemoryRouter>
        </QueryClientProvider>
      );
    });

    // Profile completion is scoped to the student shell; multi-role admins land on admin home.
    expect(await screen.findByText('admin-dashboard')).toBeInTheDocument();
  });

  it('keeps multi-workspace users on their default workspace home after the student profile is complete', async () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    setAuthenticatedSession({
      ...multiWorkspaceSession,
      userInfo: {
        ...multiWorkspaceUser,
        studentProfile: completeStudentProfile,
      },
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: null,
      preferredWorkspaceByUser: {},
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

    expect(await screen.findByText('admin-dashboard')).toBeInTheDocument();
  });
});
