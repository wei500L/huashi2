import { describe, expect, it } from 'vitest';
import type { CurrentUserVO } from './contracts';
import {
  defaultWorkspaceForUser,
  getPreferredWorkspaceForUser,
  mapPathBetweenWorkspaces,
  resolveActiveWorkspace,
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
    expect(mapPathBetweenWorkspaces('/teacher/classes', '?page=2', 'ADMIN_CONSOLE')).toBe('/admin/users');
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
});
