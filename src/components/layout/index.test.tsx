import React from 'react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import i18n from '@/lib/i18n';
import type { CurrentUserVO } from '@/lib/contracts';
import { useAuthStore, useUIStore } from '@/store';
import { Sidebar } from './index';

const originalAuthState = useAuthStore.getState();
const originalUiState = useUIStore.getState();

const multiWorkspaceUser: CurrentUserVO = {
  id: 21,
  username: 'admin.teacher',
  email: 'admin.teacher@example.com',
  displayName: 'Admin Teacher',
  primaryRole: 'ADMIN',
  roles: ['ADMIN', 'TEACHER', 'STUDENT'],
  capabilities: ['ADMIN_CONSOLE', 'TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'],
  studentProfile: null,
  teacherProfile: null,
};

const teacherOnlyUser: CurrentUserVO = {
  ...multiWorkspaceUser,
  id: 22,
  username: 'teacher.only',
  primaryRole: 'TEACHER',
  roles: ['TEACHER'],
  capabilities: ['TEACHING_WORKSPACE'],
};

const LocationProbe: React.FC = () => {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
};

describe('Sidebar workspace navigation', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: multiWorkspaceUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isSidebarCollapsed: false,
      activeWorkspace: 'ADMIN_CONSOLE',
      preferredWorkspaceByUser: {},
    });
    await i18n.changeLanguage('zh-CN');
  });

  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
  });

  it('shows only the current workspace navigation and switches route/context together', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/users']}>
        <Sidebar />
        <LocationProbe />
      </MemoryRouter>
    );

    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.queryByText('教师工作台')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '教师' }));

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/teacher/workspace');
    });

    expect(screen.getByText('教师工作台')).toBeInTheDocument();
    expect(screen.queryByText('用户管理')).not.toBeInTheDocument();
    expect(useUIStore.getState().activeWorkspace).toBe('TEACHING_WORKSPACE');
  });

  it('hides the workspace switcher for single-workspace users', () => {
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: teacherOnlyUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: 'TEACHING_WORKSPACE',
      preferredWorkspaceByUser: {},
    });

    render(
      <MemoryRouter initialEntries={['/teacher/workspace']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.queryByText('切换工作空间')).not.toBeInTheDocument();
  });
});
