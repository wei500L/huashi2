import type { Capability, CurrentUserVO, Role } from './contracts';

export type WorkspaceId = Capability;

type WorkspaceUser = Pick<CurrentUserVO, 'id' | 'username' | 'primaryRole' | 'capabilities'>;

const WORKSPACE_ORDER: WorkspaceId[] = ['ADMIN_CONSOLE', 'TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'];

const ROLE_WORKSPACE_MAP: Record<Role, WorkspaceId> = {
  ADMIN: 'ADMIN_CONSOLE',
  TEACHER: 'TEACHING_WORKSPACE',
  STUDENT: 'STUDENT_WORKSPACE',
};

export function listAvailableWorkspaces(capabilities?: Capability[] | null): WorkspaceId[] {
  if (!Array.isArray(capabilities)) {
    return [];
  }
  return WORKSPACE_ORDER.filter((workspace) => capabilities.includes(workspace));
}

export function workspacePreferenceKey(user?: Pick<CurrentUserVO, 'id' | 'username'> | null): string | null {
  if (!user) {
    return null;
  }
  return `${user.id}:${user.username}`;
}

export function workspaceFromRole(role?: Role | null): WorkspaceId | null {
  if (!role) {
    return null;
  }
  return ROLE_WORKSPACE_MAP[role] ?? null;
}

export function defaultWorkspaceForUser(
  user?: Pick<CurrentUserVO, 'primaryRole' | 'capabilities'> | null,
  preferredWorkspace?: WorkspaceId | null
): WorkspaceId | null {
  const available = listAvailableWorkspaces(user?.capabilities);
  if (!available.length) {
    return null;
  }
  if (preferredWorkspace && available.includes(preferredWorkspace)) {
    return preferredWorkspace;
  }
  if (available.includes('ADMIN_CONSOLE')) {
    return 'ADMIN_CONSOLE';
  }
  const primaryWorkspace = workspaceFromRole(user?.primaryRole);
  if (primaryWorkspace && available.includes(primaryWorkspace)) {
    return primaryWorkspace;
  }
  return available[0];
}

export function homePathForWorkspace(workspace?: WorkspaceId | null): string {
  switch (workspace) {
    case 'ADMIN_CONSOLE':
      return '/admin/dashboard';
    case 'TEACHING_WORKSPACE':
      return '/teacher/workspace';
    case 'STUDENT_WORKSPACE':
    default:
      return '/dashboard';
  }
}

export function workspaceFromPathname(pathname: string): WorkspaceId | null {
  if (pathname === '/monitor' || pathname.startsWith('/teacher')) {
    return 'TEACHING_WORKSPACE';
  }
  if (pathname.startsWith('/admin')) {
    return 'ADMIN_CONSOLE';
  }
  if (
    pathname.startsWith('/dashboard') ||
    pathname.startsWith('/diagnosis') ||
    pathname.startsWith('/training') ||
    pathname.startsWith('/assessments') ||
    pathname.startsWith('/analytics') ||
    pathname.startsWith('/errors') ||
    pathname.startsWith('/history')
  ) {
    return 'STUDENT_WORKSPACE';
  }
  return null;
}

export function isGlobalWorkspacePath(pathname: string): boolean {
  return pathname.startsWith('/settings');
}

function extractLexicalPairsSuffix(pathname: string): string | null {
  if (pathname === '/teacher/lexical-pairs' || pathname === '/admin/lexical-pairs') {
    return '';
  }
  if (pathname === '/teacher/lexical-pairs/imports' || pathname === '/admin/lexical-pairs/imports') {
    return '/imports';
  }
  if (pathname === '/teacher/lexical-pairs/new' || pathname === '/admin/lexical-pairs/new') {
    return '/new';
  }
  const editMatch = pathname.match(/^\/(?:teacher|admin)\/lexical-pairs\/([^/]+)\/edit$/);
  if (editMatch) {
    return `/${editMatch[1]}/edit`;
  }
  return null;
}

function lexicalPairsBasePathForWorkspace(workspace: WorkspaceId): string | null {
  switch (workspace) {
    case 'ADMIN_CONSOLE':
      return '/admin/lexical-pairs';
    case 'TEACHING_WORKSPACE':
      return '/teacher/lexical-pairs';
    default:
      return null;
  }
}

export function mapPathBetweenWorkspaces(pathname: string, search: string, targetWorkspace: WorkspaceId): string {
  if (isGlobalWorkspacePath(pathname)) {
    return `${pathname}${search}`;
  }

  const lexicalPairsSuffix = extractLexicalPairsSuffix(pathname);
  const lexicalPairsBasePath = lexicalPairsBasePathForWorkspace(targetWorkspace);
  if (lexicalPairsSuffix !== null && lexicalPairsBasePath) {
    return `${lexicalPairsBasePath}${lexicalPairsSuffix}${search}`;
  }

  return homePathForWorkspace(targetWorkspace);
}

export function getPreferredWorkspaceForUser(
  user: Pick<CurrentUserVO, 'id' | 'username' | 'capabilities'> | null | undefined,
  preferredWorkspaceByUser: Record<string, WorkspaceId>
): WorkspaceId | null {
  const preferenceKey = workspacePreferenceKey(user);
  if (!preferenceKey) {
    return null;
  }
  const preferredWorkspace = preferredWorkspaceByUser[preferenceKey];
  if (!preferredWorkspace) {
    return null;
  }
  return listAvailableWorkspaces(user?.capabilities).includes(preferredWorkspace) ? preferredWorkspace : null;
}

export function resolveActiveWorkspace(params: {
  user?: WorkspaceUser | null;
  pathname: string;
  activeWorkspace?: WorkspaceId | null;
  preferredWorkspace?: WorkspaceId | null;
}): WorkspaceId | null {
  const { user, pathname, activeWorkspace, preferredWorkspace } = params;
  const available = listAvailableWorkspaces(user?.capabilities);
  if (!available.length) {
    return null;
  }

  const pathWorkspace = workspaceFromPathname(pathname);
  if (pathWorkspace && available.includes(pathWorkspace)) {
    return pathWorkspace;
  }

  if (activeWorkspace && available.includes(activeWorkspace)) {
    return activeWorkspace;
  }

  return defaultWorkspaceForUser(user, preferredWorkspace);
}
