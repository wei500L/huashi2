import { describe, expect, it } from 'vitest';
import type { CurrentUserVO } from './contracts';
import {
  defaultWorkspaceForUser,
  getPreferredWorkspaceForUser,
  mapPathBetweenWorkspaces,
  resolveActiveWorkspace,
  resolveHomePathForUser,
  workspaceFromPathname,
  workspacePreferenceKey,
} from './workspaces';

const multiWorkspaceUser: Pick<CurrentUserVO, 'id' | 'username' | 'primaryRole' | 'capabilities'> = {
  id: 7,
  username: 'admin.teacher',
  primaryRole: 'ADMIN',
  capabilities: ['ADMIN_CONSOLE', 'TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'],
};

describe('workspace helpers', () => {
  it('prefers remembered workspace when it is still available', () => {
    expect(defaultWorkspaceForUser(multiWorkspaceUser, 'TEACHING_WORKSPACE')).toBe('TEACHING_WORKSPACE');
  });

  it('defaults multi-workspace users to admin when no preference exists', () => {
    expect(defaultWorkspaceForUser(multiWorkspaceUser)).toBe('ADMIN_CONSOLE');
  });

  it('ignores invalid remembered workspaces for the current user', () => {
    const preferenceKey = workspacePreferenceKey(multiWorkspaceUser);
    const preferredWorkspaceByUser = preferenceKey ? { [preferenceKey]: 'TEACHING_WORKSPACE' as const } : {};

    expect(
      getPreferredWorkspaceForUser(
        {
          ...multiWorkspaceUser,
          capabilities: ['STUDENT_WORKSPACE'],
        },
        preferredWorkspaceByUser
      )
    ).toBeNull();
  });

  it('resolves the current workspace from the pathname when possible', () => {
    expect(workspaceFromPathname('/teacher/classes/2')).toBe('TEACHING_WORKSPACE');
    expect(workspaceFromPathname('/admin/config-center')).toBe('ADMIN_CONSOLE');
    expect(workspaceFromPathname('/settings')).toBeNull();
  });

  it('maps shared lexical-pair routes between admin and teacher workspaces', () => {
    expect(
      mapPathBetweenWorkspaces('/admin/lexical-pairs/42/edit', '?tab=review', 'TEACHING_WORKSPACE')
    ).toBe('/teacher/lexical-pairs/42/edit?tab=review');

    expect(
      mapPathBetweenWorkspaces('/teacher/lexical-pairs/imports', '?batchId=9', 'ADMIN_CONSOLE')
    ).toBe('/admin/lexical-pairs/imports?batchId=9');
  });

  it('keeps global pages in place and falls back unmatched pages to the workspace home', () => {
    expect(mapPathBetweenWorkspaces('/settings', '?tab=session', 'ADMIN_CONSOLE')).toBe('/settings?tab=session');
    expect(mapPathBetweenWorkspaces('/teacher/classes', '?page=2', 'ADMIN_CONSOLE')).toBe('/admin/dashboard');
  });

  it('prefers the path workspace over remembered state, then falls back to the remembered workspace', () => {
    expect(
      resolveActiveWorkspace({
        user: multiWorkspaceUser,
        pathname: '/teacher/workspace',
        activeWorkspace: 'ADMIN_CONSOLE',
        preferredWorkspace: 'STUDENT_WORKSPACE',
      })
    ).toBe('TEACHING_WORKSPACE');

    expect(
      resolveActiveWorkspace({
        user: multiWorkspaceUser,
        pathname: '/settings',
        activeWorkspace: null,
        preferredWorkspace: 'STUDENT_WORKSPACE',
      })
    ).toBe('STUDENT_WORKSPACE');
  });

  it('keeps student-profile completion gates scoped to the student workspace', () => {
    const preferredWorkspaceByUser = {};

    expect(
      resolveHomePathForUser({
        user: {
          ...multiWorkspaceUser,
          studentProfile: {
            studentNo: 'S-001',
            gradeName: '',
            englishLevel: 'A2',
            frenchLevel: 'A1',
            courseStage: 'FOUNDATION',
            compositeScore: 0,
          },
        },
        pathname: '/login',
        activeWorkspace: null,
        preferredWorkspaceByUser,
      })
    ).toBe('/admin/dashboard');

    expect(
      resolveHomePathForUser({
        user: {
          id: 9,
          username: 'student.only',
          primaryRole: 'STUDENT',
          capabilities: ['STUDENT_WORKSPACE'],
          studentProfile: {
            studentNo: 'S-009',
            gradeName: '',
            englishLevel: 'A2',
            frenchLevel: 'A1',
            courseStage: 'FOUNDATION',
            compositeScore: 0,
          },
        },
        pathname: '/login',
        activeWorkspace: null,
        preferredWorkspaceByUser,
      })
    ).toBe('/settings');
  });

  it('does not let stale active workspace state override the default home path', () => {
    expect(
      resolveHomePathForUser({
        user: multiWorkspaceUser,
        pathname: '/login',
        activeWorkspace: 'STUDENT_WORKSPACE',
        preferredWorkspaceByUser: {},
      })
    ).toBe('/admin/dashboard');
  });

  it('defaults teachers to the teaching workspace home even when student capability is also present', () => {
    expect(
      resolveHomePathForUser({
        user: {
          id: 10,
          username: 'teacher.multi',
          primaryRole: 'TEACHER',
          capabilities: ['TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'],
          studentProfile: null,
        },
        pathname: '/login',
        activeWorkspace: 'STUDENT_WORKSPACE',
        preferredWorkspaceByUser: {},
      })
    ).toBe('/teacher/workspace');
  });
});
