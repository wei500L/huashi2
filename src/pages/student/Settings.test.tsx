import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { CurrentUserVO, LoginResponse } from '@/lib/contracts';
import { authService, studentService } from '@/lib/services';
import { readStoredSession, writeStoredSession } from '@/lib/session';
import { useAuthStore, useUIStore } from '@/store';
import SettingsPage from './Settings';

vi.mock('@/lib/services', () => ({
  authService: {
    getSessionOverview: vi.fn(),
    changePassword: vi.fn(),
  },
  studentService: {
    updateProfile: vi.fn(),
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
  studentProfile: {
    studentNo: 'S20260001',
    gradeName: '',
    frenchLevel: '',
    courseStage: '',
    compositeScore: 0,
    dailyTrainingTarget: null,
    weeklyAccuracyTarget: null,
  },
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

function renderSettingsPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <SettingsPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('SettingsPage student profile editor', () => {
  beforeEach(() => {
    window.localStorage.clear();
    writeStoredSession(mockSession);
    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: mockSession,
      user: mockUser,
      error: null,
      initialize: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      syncFromStorage: originalAuthState.syncFromStorage,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isDarkMode: false,
    });
    vi.mocked(authService.getSessionOverview).mockResolvedValue({
      hasActiveSession: true,
      accessTokenExpiresAt: '2030-01-01T00:00:00Z',
      refreshSessionIssuedAt: '2030-01-01T00:00:00Z',
      refreshSessionExpiresAt: '2030-01-02T00:00:00Z',
      userAgentFingerprint: 'ua',
      issuedIpAddress: '127.0.0.1',
    });
    vi.mocked(authService.changePassword).mockResolvedValue(undefined);
    vi.mocked(studentService.updateProfile).mockResolvedValue({
      studentNo: 'S20260001',
      gradeName: '高一（2）班',
      frenchLevel: 'A2',
      courseStage: 'INTERMEDIATE',
      compositeScore: 0,
      dailyTrainingTarget: null,
      weeklyAccuracyTarget: null,
    });
  });

  afterEach(() => {
    cleanup();
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('saves the student profile and syncs the stored session', async () => {
    renderSettingsPage();

    fireEvent.change(await screen.findByPlaceholderText('例如 高一（2）班'), { target: { value: '高一（2）班' } });
    fireEvent.change(screen.getByLabelText('法语水平'), { target: { value: 'A2' } });
    fireEvent.change(screen.getByLabelText('课程阶段'), { target: { value: 'INTERMEDIATE' } });
    fireEvent.click(screen.getByRole('button', { name: '保存资料' }));

    await waitFor(() => {
      expect(studentService.updateProfile).toHaveBeenCalledWith({
        gradeName: '高一（2）班',
        frenchLevel: 'A2',
        courseStage: 'INTERMEDIATE',
      });
    });

    expect(await screen.findByText('学生资料已更新。')).toBeInTheDocument();
    expect(readStoredSession()?.userInfo.studentProfile?.gradeName).toBe('高一（2）班');
    expect(useAuthStore.getState().user?.studentProfile?.frenchLevel).toBe('A2');
  });

  it('shows the student profile editor for users with the student workspace capability', async () => {
    const multiWorkspaceUser: CurrentUserVO = {
      ...mockUser,
      primaryRole: 'ADMIN',
      roles: ['ADMIN', 'STUDENT'],
      capabilities: ['ADMIN_CONSOLE', 'STUDENT_WORKSPACE'],
    };
    const multiWorkspaceSession: LoginResponse = {
      ...mockSession,
      userInfo: multiWorkspaceUser,
    };

    writeStoredSession(multiWorkspaceSession);
    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: multiWorkspaceSession,
      user: multiWorkspaceUser,
      error: null,
    });

    renderSettingsPage();

    expect(await screen.findByText('学生画像')).toBeInTheDocument();
    expect(screen.getByText('待补齐')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '保存资料' })).toBeInTheDocument();
  });

  it('keeps the shared settings page accessible without rendering student profile editing for teacher-only users', async () => {
    const teacherUser: CurrentUserVO = {
      ...mockUser,
      primaryRole: 'TEACHER',
      roles: ['TEACHER'],
      capabilities: ['TEACHING_WORKSPACE'],
      studentProfile: null,
      teacherProfile: {
        employeeNo: 'T20260001',
        department: 'French',
        title: 'Lecturer',
      },
    };
    const teacherSession: LoginResponse = {
      ...mockSession,
      userInfo: teacherUser,
    };

    writeStoredSession(teacherSession);
    useAuthStore.setState({
      ...useAuthStore.getState(),
      status: 'authenticated',
      session: teacherSession,
      user: teacherUser,
      error: null,
    });

    renderSettingsPage();

    expect(await screen.findByText('安全设置')).toBeInTheDocument();
    expect(screen.queryByText('学生画像')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '保存资料' })).not.toBeInTheDocument();
  });
});
