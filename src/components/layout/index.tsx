import React, { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Activity,
  BookCopy,
  BookOpen,
  Brain,
  FilePenLine,
  ChevronLeft,
  ChevronDown,
  CircleAlert,
  Database,
  GraduationCap,
  History,
  LayoutDashboard,
  LineChart,
  LogOut,
  Menu,
  Microscope,
  Moon,
  MoreVertical,
  Search,
  ShieldCheck,
  Settings,
  Shield,
  Sparkles,
  Sun,
  UserRound,
  Users,
  X,
} from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { RouteErrorBoundary } from '@/components/common/AppErrorBoundary';
import { FeedbackState } from '@/components/common/FeedbackState';
import { NotificationBell } from './NotificationBell';
import { useAuthStore, useUIStore } from '@/store';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
import type { LexicalRagAnswerVO, LexicalRagConversationMessageVO, LexicalRagQueryRequest } from '@/lib/contracts';
import { resolveRouteTitle } from '@/lib/page-title';
import { aiService } from '@/lib/services';
import { cn } from '@/lib/utils';
import { userHasCapability } from '@/lib/format';
import {
  getPreferredWorkspaceForUser,
  homePathForWorkspace,
  listAvailableWorkspaces,
  mapPathBetweenWorkspaces,
  resolveActiveWorkspace,
} from '@/lib/workspaces';
import type { WorkspaceId } from '@/lib/workspaces';

type NavItem = {
  name: string;
  path: string;
  icon: React.ComponentType<{ size?: number; className?: string; 'aria-hidden'?: boolean }>;
};

type WorkspaceMeta = {
  labelKey: string;
  icon: React.ComponentType<{ size?: number; className?: string; 'aria-hidden'?: boolean }>;
};

const WORKSPACE_META: Record<WorkspaceId, WorkspaceMeta> = {
  ADMIN_CONSOLE: {
    labelKey: 'shell.workspaces.ADMIN_CONSOLE',
    icon: Shield,
  },
  TEACHING_WORKSPACE: {
    labelKey: 'shell.workspaces.TEACHING_WORKSPACE',
    icon: GraduationCap,
  },
  STUDENT_WORKSPACE: {
    labelKey: 'shell.workspaces.STUDENT_WORKSPACE',
    icon: LayoutDashboard,
  },
};

function buildSections(t: (key: string) => string, workspace?: WorkspaceId | null): Array<{ label: string; items: NavItem[] }> {
  const sections: Array<{ label: string; items: NavItem[] }> = [];

  switch (workspace) {
    case 'ADMIN_CONSOLE':
      sections.push({
        label: t('shell.sections.admin'),
        items: [
          { name: t('shell.nav.adminDashboard'), path: '/admin/dashboard', icon: LayoutDashboard },
          { name: t('shell.nav.adminUsers'), path: '/admin/users', icon: Users },
          { name: t('shell.nav.adminAuditLogs'), path: '/admin/audit-logs', icon: History },
          { name: t('shell.nav.adminLexicalPairs'), path: '/admin/lexical-pairs', icon: BookOpen },
          { name: t('shell.nav.adminConfigCenter'), path: '/admin/config-center', icon: Shield },
        ],
      });
      break;
    case 'TEACHING_WORKSPACE':
      sections.push({
        label: t('shell.sections.teaching'),
        items: [
          { name: t('shell.nav.teacherWorkspace'), path: '/teacher/workspace', icon: LayoutDashboard },
          { name: t('shell.nav.teacherClasses'), path: '/teacher/classes', icon: Users },
          { name: t('shell.nav.teacherAssessments'), path: '/teacher/assessments', icon: FilePenLine },
          { name: t('shell.nav.teacherResearch'), path: '/teacher/research', icon: Microscope },
          { name: t('shell.nav.teacherTemplates'), path: '/teacher/diagnosis-templates', icon: Brain },
          { name: t('shell.nav.teacherLexicalPairs'), path: '/teacher/lexical-pairs', icon: BookOpen },
          { name: t('shell.nav.teacherLexicalLists'), path: '/teacher/lexical-lists', icon: BookCopy },
          { name: t('shell.nav.teacherInterventions'), path: '/teacher/interventions', icon: Shield },
        ],
      });
      break;
    case 'STUDENT_WORKSPACE':
      sections.push({
        label: t('shell.sections.core'),
        items: [
          { name: t('shell.nav.dashboard'), path: '/dashboard', icon: LayoutDashboard },
          { name: t('shell.nav.diagnosis'), path: '/diagnosis', icon: Activity },
          { name: t('shell.nav.training'), path: '/training', icon: GraduationCap },
          { name: t('shell.nav.assessments'), path: '/assessments', icon: BookCopy },
          { name: t('shell.nav.studentResearch'), path: '/student/research', icon: Microscope },
          { name: t('shell.nav.analytics'), path: '/analytics', icon: LineChart },
          { name: t('shell.nav.errors'), path: '/errors', icon: Database },
          { name: t('shell.nav.history'), path: '/history', icon: History },
        ],
      });
      break;
    default:
      break;
  }

  sections.push({
    label: t('shell.sections.system'),
    items: [{ name: t('shell.nav.settings'), path: '/settings', icon: Settings }],
  });

  return sections;
}

type SidebarContentProps = {
  isCollapsed: boolean;
  navigationLabel: string;
  headerAction?: React.ReactNode;
  onNavigate?: () => void;
};

type WorkspaceSwitcherProps = {
  activeWorkspace: WorkspaceId;
  isCollapsed: boolean;
  workspaces: WorkspaceId[];
  onSelect: (workspace: WorkspaceId) => void;
};

const WorkspaceSwitcher: React.FC<WorkspaceSwitcherProps> = ({ activeWorkspace, isCollapsed, workspaces, onSelect }) => {
  const { t } = useTranslation();

  if (workspaces.length < 2) {
    return null;
  }

  return (
    <div className={cn(isCollapsed ? 'px-2 pt-1 pb-2' : 'px-3 pt-1 pb-2')}>
      {!isCollapsed && (
        <div className="px-3 text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-400 dark:text-white/35">
          {t('shell.switchWorkspaceLabel')}
        </div>
      )}
      <div
        className={cn(
          'mt-2 rounded-xl border border-border-subtle bg-surface-sunken/80',
          isCollapsed ? 'p-1.5 space-y-1' : 'p-1.5 space-y-1'
        )}
      >
        {workspaces.map((workspace) => {
          const meta = WORKSPACE_META[workspace];
          const isActive = workspace === activeWorkspace;
          return (
            <button
              key={workspace}
              type="button"
              aria-pressed={isActive}
              aria-label={t(meta.labelKey)}
              title={isCollapsed ? t(meta.labelKey) : undefined}
              onClick={() => onSelect(workspace)}
              className={cn(
                'w-full min-h-11 rounded-lg border text-left motion-feedback',
                isCollapsed ? 'flex items-center justify-center px-2 py-2.5' : 'flex items-center gap-3 px-3 py-2.5',
                isActive
                  ? 'border-primary/30 bg-primary/[0.1] text-primary shadow-sm'
                  : 'border-transparent text-slate-500 hover:border-border-strong hover:bg-surface-raised hover:text-slate-900 dark:text-white/55 dark:hover:text-white'
              )}
            >
              <meta.icon size={17} aria-hidden={true} />
              {!isCollapsed && (
                <div className="min-w-0 flex-1">
                  <div className="truncate text-xs font-semibold tracking-wide">{t(meta.labelKey)}</div>
                  <div className="mt-0.5 truncate text-[10px] uppercase tracking-[0.16em] text-slate-400 dark:text-white/35">
                    {isActive ? t('shell.currentWorkspaceLabel') : t('shell.switchWorkspaceLabel')}
                  </div>
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};

const SidebarContent: React.FC<SidebarContentProps> = ({ isCollapsed, navigationLabel, headerAction, onNavigate }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { activeWorkspace, preferredWorkspaceByUser, setActiveWorkspace } = useUIStore();
  const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
  const currentWorkspace = useMemo(
    () =>
      resolveActiveWorkspace({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspace,
      }),
    [user, location.pathname, activeWorkspace, preferredWorkspace]
  );
  const availableWorkspaces = useMemo(() => listAvailableWorkspaces(user?.capabilities), [user?.capabilities]);
  const sections = useMemo(() => buildSections(t, currentWorkspace), [t, currentWorkspace]);
  const homePath = homePathForWorkspace(currentWorkspace);
  const roleLabels = (user?.roles || []).map((role) => t(`shell.roles.${role}`));
  const researchNavigationActive = location.pathname === '/teacher/research'
    || (location.pathname.startsWith('/teacher/assessments/')
      && new URLSearchParams(location.search).get('context') === 'research');
  const resolveNavActive = (item: NavItem, routerActive: boolean) => {
    if (!researchNavigationActive) {
      return routerActive;
    }
    if (item.path === '/teacher/research') {
      return true;
    }
    if (item.path === '/teacher/assessments') {
      return false;
    }
    return routerActive;
  };

  const handleWorkspaceSelect = (workspace: WorkspaceId) => {
    const targetPath = mapPathBetweenWorkspaces(location.pathname, location.search, workspace);
    setActiveWorkspace(workspace, user);
    onNavigate?.();
    if (targetPath !== `${location.pathname}${location.search}`) {
      navigate(targetPath);
    }
  };

  return (
    <>
      <div
        className={cn(
          'relative z-10 border-b border-border-subtle',
          isCollapsed ? 'px-3 pb-4 pt-5 flex flex-col items-center gap-3' : 'px-5 pb-5 pt-6 flex items-center justify-between gap-3'
        )}
      >
        <Link
          to={homePath}
          className={cn('flex items-center', isCollapsed ? 'justify-center' : 'gap-3')}
          aria-label={t('common.appName')}
          onClick={onNavigate}
        >
          <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
            <Sparkles size={16} aria-hidden="true" />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col">
              <span className="text-base font-semibold tracking-tight text-slate-900 dark:text-white leading-none">
                EF<span className="text-primary">.</span>Transfer
              </span>
              <span className="mt-1 truncate text-[10px] font-medium text-slate-400 dark:text-white/40 leading-none">
                {t('shell.brandDescriptor')}
              </span>
            </div>
          )}
        </Link>
        {headerAction && <div className={cn(isCollapsed ? 'flex justify-center' : 'shrink-0')}>{headerAction}</div>}
      </div>

      <nav
        role="navigation"
        aria-label={navigationLabel}
        className={cn(
          'min-h-0 flex-1 overflow-y-auto no-scrollbar relative z-10',
          isCollapsed ? 'px-2 py-4 space-y-5' : 'px-3 py-5 space-y-7'
        )}
      >
        {currentWorkspace && (
          <WorkspaceSwitcher
            activeWorkspace={currentWorkspace}
            isCollapsed={isCollapsed}
            workspaces={availableWorkspaces}
            onSelect={handleWorkspaceSelect}
          />
        )}
        {sections.map((section) => (
          <div key={section.label} className={cn(isCollapsed ? 'space-y-1' : 'space-y-2')}>
            {!isCollapsed && (
              <h4 className="px-3 text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-400 dark:text-white/35">
                {section.label}
              </h4>
            )}
            <div className="space-y-0.5">
              {section.items.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  aria-label={item.name}
                  title={isCollapsed ? item.name : undefined}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex min-h-11 items-center rounded-lg border motion-feedback',
                      isCollapsed ? 'justify-center px-2.5' : 'gap-3 px-3',
                      resolveNavActive(item, isActive)
                        ? 'border-primary/25 bg-primary/[0.1] text-primary font-semibold shadow-sm'
                        : 'border-transparent text-slate-500 hover:border-border-subtle hover:bg-surface-sunken hover:text-slate-900 dark:text-white/55 dark:hover:text-white'
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      <span
                        aria-hidden="true"
                        className={cn(
                          'absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-full bg-primary motion-feedback',
                          resolveNavActive(item, isActive) ? 'opacity-100' : 'opacity-0 group-hover:opacity-40'
                        )}
                      />
                      <item.icon size={17} className="shrink-0" aria-hidden={true} />
                      {!isCollapsed && <span className="min-w-0 flex-1 break-words text-sm leading-5">{item.name}</span>}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div
        className={cn(
          'relative z-10 mt-auto border-t border-border-subtle bg-surface-raised/60',
          isCollapsed ? 'p-2.5' : 'p-3 space-y-3'
        )}
      >
        {!isCollapsed && user && (
          <div className="flex min-w-0 items-center gap-3 rounded-xl border border-border-subtle bg-surface p-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
              <UserRound size={17} aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-slate-900 dark:text-white/90">{user.displayName}</div>
              <div className="mt-0.5 truncate text-[10px] uppercase tracking-[0.16em] text-slate-400 dark:text-white/35">
                {roleLabels.join(' / ') || user.username}
              </div>
            </div>
          </div>
        )}
        <button
          type="button"
          aria-label={t('common.actions.signOut')}
          title={isCollapsed ? t('common.actions.signOut') : undefined}
          onClick={() => {
            onNavigate?.();
            void logout();
          }}
          className={cn(
            'flex min-h-11 items-center rounded-lg border border-transparent text-rose-500 motion-feedback hover:border-rose-500/20 hover:bg-rose-500/10 dark:text-rose-400',
            isCollapsed ? 'w-full justify-center px-2.5' : 'w-full gap-3 px-3'
          )}
        >
          <LogOut size={17} aria-hidden="true" />
          {!isCollapsed && <span className="text-sm font-semibold">{t('common.actions.signOut')}</span>}
        </button>
      </div>
    </>
  );
};

export const Sidebar: React.FC = () => {
  const { t } = useTranslation();
  const { isSidebarCollapsed, toggleSidebar } = useUIStore();

  return (
    <aside
      aria-label={t('shell.mobileNavigationTitle')}
      className={cn(
        'sidebar-shell sticky top-0 hidden h-screen shrink-0 flex-col border-r border-border-subtle bg-surface transition-all motion-layout duration-200 lg:flex',
        isSidebarCollapsed ? 'w-20' : 'w-72'
      )}
    >
      <SidebarContent
        isCollapsed={isSidebarCollapsed}
        navigationLabel={t('shell.mobileNavigationTitle')}
        headerAction={
          <button
            type="button"
            aria-label={isSidebarCollapsed ? t('common.actions.expandSidebar') : t('common.actions.collapseSidebar')}
            title={isSidebarCollapsed ? t('common.actions.expandSidebar') : t('common.actions.collapseSidebar')}
            onClick={toggleSidebar}
            className="rounded-lg border border-border-subtle p-2 text-slate-500 hover:border-border-strong hover:bg-surface-sunken hover:text-slate-900 dark:text-white/65 dark:hover:text-white"
          >
            <ChevronLeft
              className={cn(
                'transition-transform duration-200',
                isSidebarCollapsed && 'rotate-180'
              )}
              size={18}
              aria-hidden="true"
            />
          </button>
        }
      />
    </aside>
  );
};

const MobileSidebarDrawer: React.FC = () => {
  const { t } = useTranslation();
  const { isMobileSidebarOpen, closeMobileSidebar } = useUIStore();
  const reducedMotion = useReducedMotion();
  const drawerRef = React.useRef<HTMLElement | null>(null);
  const closeButtonRef = React.useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(min-width: 1024px)');
    const syncDrawerState = (matches: boolean) => {
      if (matches) {
        closeMobileSidebar();
      }
    };
    const handleChange = (event: MediaQueryListEvent) => syncDrawerState(event.matches);

    syncDrawerState(mediaQuery.matches);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, [closeMobileSidebar]);

  useBodyScrollLock(isMobileSidebarOpen);
  useDialogAccessibility({
    open: isMobileSidebarOpen,
    containerRef: drawerRef,
    initialFocusRef: closeButtonRef,
    onClose: closeMobileSidebar,
  });

  return createPortal(
    <AnimatePresence>
      {isMobileSidebarOpen && (
        <>
          <motion.div
            aria-hidden="true"
            initial={reducedMotion ? false : { opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeMobileSidebar}
            className="fixed inset-0 z-[70] bg-slate-950/40 lg:hidden"
          />
          <motion.aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="mobile-sidebar-title"
            tabIndex={-1}
            initial={reducedMotion ? false : { x: '-100%' }}
            animate={{ x: 0 }}
            exit={reducedMotion ? undefined : { x: '-100%' }}
            /* A short, deterministic reveal keeps navigation responsive. */
            transition={reducedMotion ? { duration: 0 } : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
            className="sidebar-shell safe-area-drawer surface-panel fixed left-3 top-3 z-[80] flex w-[min(22rem,calc(100vw-1.5rem))] flex-col rounded-xl lg:hidden"
          >
            <h2 id="mobile-sidebar-title" className="sr-only">{t('shell.mobileNavigationTitle')}</h2>
            <SidebarContent
              isCollapsed={false}
              navigationLabel={t('shell.mobileNavigationTitle')}
              onNavigate={closeMobileSidebar}
              headerAction={
                <button
                  ref={closeButtonRef}
                  type="button"
                  aria-label={t('common.actions.closeNavigation')}
                  onClick={closeMobileSidebar}
                  className="flex min-h-11 min-w-11 items-center justify-center rounded-xl border border-slate-200 p-2.5 hover:bg-black/5 dark:border-white/5 dark:hover:bg-white/10"
                >
                  <X size={20} className="text-slate-400 dark:text-white/70" />
                </button>
              }
            />
          </motion.aside>
        </>
      )}
    </AnimatePresence>,
    document.body
  );
};

const lexicalRagConversationListKey = ['lexical-rag-conversations'] as const;
const lexicalRagConversationDetailKey = (conversationId: string | null) => ['lexical-rag-conversation', conversationId] as const;

function formatAssistantTimestamp(value: string | null | undefined, locale: string): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return new Intl.DateTimeFormat(locale, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

type AssistantResponsePanelProps = {
  locale: string;
  payload: LexicalRagAnswerVO;
  t: (key: string) => string;
  timestamp?: string | null;
};

const AssistantResponsePanel: React.FC<AssistantResponsePanelProps> = ({ locale, payload, t, timestamp }) => {
  const confidencePercent = Math.round(Math.max(0, Math.min(1, payload.confidence)) * 100);
  const contextByCitationId = new Map((payload.contextChunks ?? []).map((chunk) => [chunk.citationId, chunk]));
  const GroundingIcon = payload.grounded ? ShieldCheck : CircleAlert;

  return (
    <section className="overflow-hidden rounded-3xl border border-[hsl(var(--ai)/0.24)] bg-[hsl(var(--ai)/0.045)] shadow-sm">
      <header className="border-b border-[hsl(var(--ai)/0.14)] bg-[hsl(var(--ai)/0.06)] px-5 py-4">
        <div className="flex items-start justify-between gap-4">
          <div className="flex min-w-0 items-start gap-3">
            <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[hsl(var(--ai)/0.14)] text-[hsl(var(--ai))]">
              <Brain size={17} aria-hidden="true" />
            </span>
            <div className="min-w-0">
              <div className="text-[10px] font-bold uppercase tracking-[0.28em] text-[hsl(var(--ai))]">{t('shell.answer')}</div>
              <div className="mt-1 text-base font-black text-slate-900 dark:text-white">{t('shell.lexicalAssistant')}</div>
              {timestamp && <div className="mt-1 text-[11px] text-slate-500 dark:text-white/45">{formatAssistantTimestamp(timestamp, locale)}</div>}
            </div>
          </div>
          <span className={cn(
            'inline-flex shrink-0 items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.14em]',
            payload.grounded
              ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
              : 'border-amber-500/25 bg-amber-500/10 text-amber-800 dark:text-amber-200'
          )}>
            <GroundingIcon size={13} aria-hidden="true" />
            {payload.grounded ? t('shell.grounded') : t('shell.unverified')}
          </span>
        </div>
        <div className="mt-4 flex flex-wrap gap-2 text-[11px] text-slate-500 dark:text-white/50">
          <span className="rounded-full border border-[hsl(var(--ai)/0.18)] bg-white/60 px-2.5 py-1 dark:bg-white/5">{t('shell.confidence')}: {confidencePercent}%</span>
          <span className="rounded-full border border-slate-200/80 bg-white/60 px-2.5 py-1 dark:border-white/10 dark:bg-white/5">{t('shell.source')}: {payload.generationSource}</span>
          {payload.model && <span className="rounded-full border border-slate-200/80 bg-white/60 px-2.5 py-1 dark:border-white/10 dark:bg-white/5">{t('shell.model')}: {payload.model}</span>}
        </div>
      </header>

      <div className="space-y-5 px-5 py-5">
        <div className="border-l-4 border-[hsl(var(--ai))] pl-4">
          <p className="whitespace-pre-wrap text-lg font-semibold leading-8 text-slate-900 dark:text-white">{payload.answer}</p>
        </div>

        <section className="rounded-2xl border border-slate-200/80 bg-white/70 px-4 py-4 dark:border-white/10 dark:bg-white/5" aria-labelledby={`assistant-explanation-${payload.requestId}`}>
          <div id={`assistant-explanation-${payload.requestId}`} className="text-[10px] font-bold uppercase tracking-[0.24em] text-slate-500 dark:text-white/45">{t('shell.explanation')}</div>
          <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-white/70">{payload.explanation}</p>
        </section>

        {!!payload.citations?.length && (
          <section aria-labelledby={`assistant-citations-${payload.requestId}`}>
            <div className="mb-3 flex items-end justify-between gap-3">
              <div>
                <div className="text-[10px] font-bold uppercase tracking-[0.24em] text-[hsl(var(--ai))]">{t('shell.evidence')}</div>
                <div id={`assistant-citations-${payload.requestId}`} className="mt-1 text-sm font-bold text-slate-900 dark:text-white">{t('shell.citations')}</div>
                <div className="mt-1 text-xs text-slate-500 dark:text-white/45">{t('shell.citationDetails')}</div>
              </div>
              <span className="text-xs font-bold text-[hsl(var(--ai))]">{payload.citations.length}</span>
            </div>
            <ol className="space-y-2">
              {payload.citations.map((citation, index) => {
                const context = contextByCitationId.get(citation.citationId);
                return (
                  <li key={citation.citationId}>
                    <details className="group rounded-2xl border border-slate-200/80 bg-white/65 dark:border-white/10 dark:bg-white/5">
                      <summary className="flex cursor-pointer list-none items-center gap-3 px-4 py-3 [&::-webkit-details-marker]:hidden">
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[hsl(var(--ai)/0.12)] text-[11px] font-black text-[hsl(var(--ai))]">{index + 1}</span>
                        <span className="min-w-0 flex-1">
                          <span className="block truncate text-sm font-bold text-slate-900 dark:text-white">{citation.title || citation.sourceId}</span>
                          <span className="mt-0.5 block text-[11px] text-slate-500 dark:text-white/45">{citation.citationId} · {citation.sourceType}</span>
                        </span>
                        <ChevronDown size={16} aria-hidden="true" className="shrink-0 text-slate-400 transition-transform group-open:rotate-180" />
                      </summary>
                      <div className="space-y-3 border-t border-slate-200/70 px-4 py-4 text-sm dark:border-white/10">
                        <p className="whitespace-pre-wrap leading-6 text-slate-700 dark:text-white/70">{citation.snippet}</p>
                        <dl className="grid gap-2 text-xs text-slate-500 dark:text-white/45 sm:grid-cols-3">
                          <div><dt className="font-bold uppercase tracking-[0.12em]">{t('shell.sourceType')}</dt><dd className="mt-1 break-words">{citation.sourceType}</dd></div>
                          <div><dt className="font-bold uppercase tracking-[0.12em]">{t('shell.sourceId')}</dt><dd className="mt-1 break-words">{citation.sourceId}</dd></div>
                          {citation.score != null && <div><dt className="font-bold uppercase tracking-[0.12em]">{t('shell.relevance')}</dt><dd className="mt-1">{Math.round(citation.score * 100)}%</dd></div>}
                        </dl>
                        {context && <div className="rounded-xl border border-[hsl(var(--ai)/0.16)] bg-[hsl(var(--ai)/0.05)] px-3 py-3"><div className="text-[10px] font-bold uppercase tracking-[0.18em] text-[hsl(var(--ai))]">{t('shell.context')}</div><p className="mt-2 whitespace-pre-wrap leading-6 text-slate-700 dark:text-white/70">{context.content || context.snippet}</p></div>}
                      </div>
                    </details>
                  </li>
                );
              })}
            </ol>
          </section>
        )}

        {!!payload.contextChunks?.length && !payload.citations?.length && (
          <section aria-labelledby={`assistant-context-${payload.requestId}`}>
            <div className="mb-1 text-[10px] font-bold uppercase tracking-[0.24em] text-[hsl(var(--ai))]">{t('shell.evidence')}</div>
            <div id={`assistant-context-${payload.requestId}`} className="mb-3 text-sm font-bold text-slate-900 dark:text-white">{t('shell.context')}</div>
            <div className="space-y-2">
              {payload.contextChunks.map((chunk) => <details key={`${chunk.citationId}-${chunk.sourceId}`} className="rounded-2xl border border-slate-200/80 bg-white/65 px-4 py-3 dark:border-white/10 dark:bg-white/5"><summary className="flex cursor-pointer list-none items-center justify-between gap-3 font-bold text-slate-900 dark:text-white [&::-webkit-details-marker]:hidden">{chunk.title || chunk.sourceId}<ChevronDown size={16} aria-hidden="true" /></summary><p className="mt-3 whitespace-pre-wrap border-t border-slate-200/70 pt-3 text-sm leading-6 text-slate-600 dark:border-white/10 dark:text-white/65">{chunk.content || chunk.snippet}</p></details>)}
            </div>
          </section>
        )}

        {!!payload.recommendedActions?.length && (
          <section className="rounded-2xl border border-primary/20 bg-primary/[0.045] px-4 py-4" aria-labelledby={`assistant-actions-${payload.requestId}`}>
            <div id={`assistant-actions-${payload.requestId}`} className="text-[10px] font-bold uppercase tracking-[0.24em] text-primary">{t('shell.actions')}</div>
            <ol className="mt-3 space-y-2">
              {payload.recommendedActions.map((item, index) => <li key={`${index}-${item}`} className="flex items-start gap-3 rounded-xl border border-primary/10 bg-white/60 px-3 py-3 text-sm leading-6 text-slate-700 dark:bg-white/5 dark:text-white/70"><span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-black text-primary">{index + 1}</span><span className="break-words">{item}</span></li>)}
            </ol>
          </section>
        )}

        {payload.fallbackReason && (
          <div role="note" className="flex items-start gap-3 rounded-2xl border border-amber-500/25 bg-amber-500/[0.08] px-4 py-4 text-amber-900 dark:text-amber-100">
            <CircleAlert size={17} className="mt-0.5 shrink-0" aria-hidden="true" />
            <div><div className="text-[10px] font-bold uppercase tracking-[0.22em]">{t('shell.fallbackReason')}</div><p className="mt-1 text-sm leading-6">{payload.fallbackReason}{payload.fallbackDetail ? ` — ${payload.fallbackDetail}` : ''}</p><p className="mt-2 text-xs leading-5 opacity-75">{t('shell.fallbackDescription')}</p></div>
          </div>
        )}
      </div>
    </section>
  );
};

type ConversationMessageCardProps = {
  locale: string;
  message: LexicalRagConversationMessageVO;
  t: (key: string) => string;
};

const ConversationMessageCard: React.FC<ConversationMessageCardProps> = ({ locale, message, t }) => {
  if (message.role === 'assistant' && message.assistantPayload) {
    return <AssistantResponsePanel locale={locale} payload={message.assistantPayload} t={t} timestamp={message.createdAt} />;
  }

  return (
    <section className="rounded-3xl border border-primary/15 bg-primary/[0.06] px-5 py-4">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs uppercase tracking-[0.3em] text-primary/80">{t('shell.messages')}</div>
        {message.createdAt && (
          <div className="text-[11px] text-slate-400 dark:text-white/35">{formatAssistantTimestamp(message.createdAt, locale)}</div>
        )}
      </div>
      <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-white/80">{message.content}</p>
    </section>
  );
};

const AssistantDrawer: React.FC = () => {
  const { t, i18n } = useTranslation();
  const reducedMotion = useReducedMotion();
  const location = useLocation();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const {
    activeWorkspace,
    preferredWorkspaceByUser,
    isAssistantOpen,
    assistantDraft,
    activeAssistantConversationId,
    closeAssistant,
    setAssistantDraft,
    setActiveAssistantConversation,
  } = useUIStore();
  const [query, setQuery] = useState(assistantDraft);
  const [isStartingNewConversation, setIsStartingNewConversation] = useState(false);
  const drawerRef = React.useRef<HTMLElement | null>(null);
  const closeButtonRef = React.useRef<HTMLButtonElement | null>(null);
  const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
  const currentWorkspace = useMemo(
    () =>
      resolveActiveWorkspace({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspace,
      }),
    [user, location.pathname, activeWorkspace, preferredWorkspace]
  );
  const canUseAssistant = currentWorkspace === 'STUDENT_WORKSPACE' && userHasCapability(user, 'STUDENT_WORKSPACE');
  const assistantOpen = canUseAssistant && isAssistantOpen;
  const conversationListQuery = useQuery({
    queryKey: lexicalRagConversationListKey,
    queryFn: () => aiService.listLexicalRagConversations({ pageNo: 1, pageSize: 20 }),
    enabled: assistantOpen,
  });
  const conversationDetailQuery = useQuery({
    queryKey: lexicalRagConversationDetailKey(activeAssistantConversationId),
    queryFn: () => aiService.getLexicalRagConversation(activeAssistantConversationId as string),
    enabled: assistantOpen && !!activeAssistantConversationId,
  });
  const ragMutation = useMutation({
    mutationFn: (payload: LexicalRagQueryRequest) => aiService.queryLexicalRag(payload),
    onSuccess: async (payload) => {
      setIsStartingNewConversation(false);
      setActiveAssistantConversation(payload.conversationId);
      setAssistantDraft('');
      setQuery('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: lexicalRagConversationListKey }),
        queryClient.invalidateQueries({ queryKey: lexicalRagConversationDetailKey(payload.conversationId) }),
      ]);
    },
  });

  useEffect(() => {
    setQuery(assistantDraft);
  }, [assistantDraft]);

  useEffect(() => {
    if (!canUseAssistant && isAssistantOpen) {
      closeAssistant();
    }
  }, [canUseAssistant, isAssistantOpen, closeAssistant]);

  useEffect(() => {
    if (!assistantOpen || isStartingNewConversation || activeAssistantConversationId || assistantDraft.trim()) {
      return;
    }
    const firstConversation = conversationListQuery.data?.records?.[0];
    if (firstConversation) {
      setActiveAssistantConversation(firstConversation.conversationId);
    }
  }, [
    assistantOpen,
    isStartingNewConversation,
    activeAssistantConversationId,
    assistantDraft,
    conversationListQuery.data?.records,
    setActiveAssistantConversation,
  ]);

  useEffect(() => {
    if (activeAssistantConversationId) {
      setIsStartingNewConversation(false);
    }
  }, [activeAssistantConversationId]);

  useBodyScrollLock(assistantOpen);
  useDialogAccessibility({
    open: assistantOpen,
    containerRef: drawerRef,
    initialFocusRef: closeButtonRef,
    onClose: closeAssistant,
  });

  if (!canUseAssistant) {
    return null;
  }

  const activeMessages = conversationDetailQuery.data?.messages ?? [];
  const locale = i18n.resolvedLanguage || i18n.language || 'zh-CN';

  const handleNewConversation = () => {
    setIsStartingNewConversation(true);
    setActiveAssistantConversation(null);
    setAssistantDraft('');
    setQuery('');
  };

  const handleConversationSelect = (conversationId: string) => {
    setIsStartingNewConversation(false);
    setActiveAssistantConversation(conversationId);
    setAssistantDraft('');
    setQuery('');
  };

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) {
      return;
    }
    setAssistantDraft(trimmed);
    ragMutation.mutate({
      query: trimmed,
      conversationId: activeAssistantConversationId,
    });
  };

  return createPortal(
    <AnimatePresence>
      {assistantOpen && (
        <>
          <motion.div
            aria-hidden="true"
            initial={reducedMotion ? false : { opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeAssistant}
            className="fixed inset-0 z-[70] bg-slate-950/40"
          />
          <motion.aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="assistant-drawer-title"
            tabIndex={-1}
            initial={reducedMotion ? false : { x: '100%' }}
            animate={{ x: 0 }}
            exit={reducedMotion ? undefined : { x: '100%' }}
            transition={reducedMotion ? { duration: 0 } : { type: 'spring', stiffness: 260, damping: 28 }}
            className="safe-area-drawer-panel surface-panel fixed right-0 top-0 z-[80] h-[100dvh] max-h-[100dvh] w-full max-w-6xl overflow-y-auto border-l border-[hsl(var(--ai)/0.28)] p-4 md:p-6"
          >
            <div className="flex items-center justify-between mb-6">
              <div>
                <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-2">{t('shell.lexicalRag')}</div>
                <h3 id="assistant-drawer-title" className="text-2xl font-black text-slate-900 dark:text-white">{t('shell.lexicalAssistant')}</h3>
              </div>
              <button
                ref={closeButtonRef}
                type="button"
                aria-label={t('common.actions.close')}
                onClick={closeAssistant}
                className="p-3 rounded-2xl border border-slate-200 dark:border-white/10 hover:bg-black/5 dark:hover:bg-white/5"
              >
                <X size={18} />
              </button>
            </div>
            <div className="grid gap-4 lg:grid-cols-[18rem_minmax(0,1fr)]">
              <aside className="rounded-[2rem] border border-slate-200 dark:border-white/10 bg-white/55 dark:bg-white/5 p-4">
                <div className="flex items-center justify-between gap-3 mb-4">
                  <div>
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('shell.recentConversations')}</div>
                    <div className="mt-2 text-sm font-black text-slate-900 dark:text-white">{t('shell.conversationHistory')}</div>
                  </div>
                  <button
                    type="button"
                    onClick={handleNewConversation}
                    className="rounded-2xl border border-slate-200 dark:border-white/10 px-3 py-2 text-[11px] font-black uppercase tracking-[0.2em] text-slate-500 dark:text-white/70 hover:border-[hsl(var(--ai)/0.4)] hover:text-[hsl(var(--ai))]"
                  >
                    {t('shell.newConversation')}
                  </button>
                </div>

                {conversationListQuery.isLoading && (
                  <div className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-3 text-sm text-slate-500 dark:text-white/60">
                    {t('common.loading.searchingKnowledge')}
                  </div>
                )}

                {conversationListQuery.error && (
                  <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                    {conversationListQuery.error.message}
                  </div>
                )}

                {!conversationListQuery.isLoading && !conversationListQuery.data?.records?.length && (
                  <div className="rounded-2xl border border-dashed border-slate-200 dark:border-white/10 px-4 py-6 text-sm text-slate-500 dark:text-white/60">
                    {t('shell.noConversations')}
                  </div>
                )}

                <div className="space-y-2">
                  {conversationListQuery.data?.records?.map((conversation) => {
                    const isActive = activeAssistantConversationId === conversation.conversationId;
                    return (
                      <button
                        key={conversation.conversationId}
                        type="button"
                        aria-pressed={isActive}
                        onClick={() => handleConversationSelect(conversation.conversationId)}
                        className={cn(
                          'w-full rounded-2xl border px-4 py-3 text-left transition-colors',
                          isActive
                            ? 'border-[hsl(var(--ai)/0.3)] bg-[hsl(var(--ai)/0.09)] text-[hsl(var(--ai))]'
                            : 'border-slate-200/70 bg-white/60 text-slate-600 hover:border-[hsl(var(--ai)/0.3)] dark:border-white/10 dark:bg-slate-950/20 dark:text-white/70'
                        )}
                      >
                        <div className="text-sm font-black leading-5">{conversation.title}</div>
                        {conversation.lastMessageAt && (
                          <div className="mt-2 text-[11px] text-slate-400 dark:text-white/35">
                            {formatAssistantTimestamp(conversation.lastMessageAt, locale)}
                          </div>
                        )}
                      </button>
                    );
                  })}
                </div>
              </aside>

              <section className="rounded-[2rem] border border-slate-200 dark:border-white/10 bg-white/55 dark:bg-white/5 p-4 md:p-5">
                <div className="flex items-center justify-between gap-3 mb-4">
                  <div>
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('shell.messages')}</div>
                    <div className="mt-2 text-lg font-black text-slate-900 dark:text-white">
                      {conversationDetailQuery.data?.title || t('shell.continueConversation')}
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  {conversationDetailQuery.isLoading && activeAssistantConversationId && (
                    <div className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                      {t('shell.loadingConversation')}
                    </div>
                  )}

                  {!activeAssistantConversationId && (
                    <div className="rounded-3xl border border-dashed border-slate-200 dark:border-white/10 p-6 text-sm leading-6 text-slate-500 dark:text-white/60">
                      {assistantDraft.trim() ? t('shell.conversationDraftHint') : t('shell.conversationEmpty')}
                    </div>
                  )}

                  {conversationDetailQuery.error && (
                    <div className="rounded-3xl border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
                      {conversationDetailQuery.error.message}
                    </div>
                  )}

                  {activeMessages.map((message) => (
                    <ConversationMessageCard key={message.messageId} locale={locale} message={message} t={t} />
                  ))}

                  {ragMutation.isPending && (
                    <div role="status" aria-live="polite" className="rounded-3xl border border-[hsl(var(--ai)/0.18)] bg-[hsl(var(--ai)/0.06)] p-5">
                      <div className="text-[10px] font-bold uppercase tracking-[0.24em] text-[hsl(var(--ai))]">{t('shell.evidence')}</div>
                      <div className="mt-2 text-sm font-semibold text-slate-800 dark:text-white/85">{t('common.loading.searchingKnowledge')}</div>
                      <div className="mt-1 text-xs leading-5 text-slate-500 dark:text-white/45">{t('shell.pendingDescription')}</div>
                    </div>
                  )}

                  {ragMutation.error && (
                    <FeedbackState
                      kind="retry"
                      compact
                      className="rounded-3xl"
                      title={t('shell.requestFailed')}
                      description={ragMutation.error.message}
                      impact={t('shell.requestFailedImpact')}
                      nextStep={t('shell.requestFailedNextStep')}
                      primaryAction={{
                        label: t('shell.retryRequest'),
                        onClick: () => {
                          const trimmed = query.trim();
                          if (trimmed) {
                            setAssistantDraft(trimmed);
                            ragMutation.mutate({ query: trimmed, conversationId: activeAssistantConversationId });
                          }
                        },
                      }}
                    />
                  )}
                </div>

                <form onSubmit={submit} className="space-y-4 mt-6">
                  <textarea
                    aria-label={t('shell.drawerPromptLabel')}
                    value={query}
                    onChange={(event) => {
                      setQuery(event.target.value);
                      setAssistantDraft(event.target.value);
                    }}
                    rows={4}
                    placeholder={t('shell.drawerPromptPlaceholder')}
                    className="w-full rounded-3xl bg-white/70 dark:bg-slate-950/50 border border-slate-200 dark:border-white/10 px-5 py-4 outline-none focus:border-[hsl(var(--ai)/0.5)]"
                  />
                  <div className="flex items-center justify-between gap-3">
                    <div className="text-xs text-slate-400 dark:text-white/35">
                      {activeAssistantConversationId ? t('shell.continueConversation') : t('shell.newConversation')}
                    </div>
                    <button type="submit" disabled={ragMutation.isPending} className="btn-liquid px-6 py-3 text-white disabled:opacity-70">
                      {t('common.actions.search')}
                    </button>
                  </div>
                </form>
              </section>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>,
    document.body
  );
};

export const Topbar: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const {
    activeWorkspace,
    preferredWorkspaceByUser,
    isDarkMode,
    toggleDarkMode,
    locale,
    setLocale,
    openAssistant,
    assistantDraft,
    setAssistantDraft,
    openMobileSidebar,
  } = useUIStore();
  const [search, setSearch] = useState(assistantDraft);
  const [moreOpen, setMoreOpen] = useState(false);
  const moreMenuRef = React.useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setSearch(assistantDraft);
  }, [assistantDraft]);

  useEffect(() => {
    setMoreOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!moreOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!moreMenuRef.current?.contains(target)) {
        setMoreOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMoreOpen(false);
      }
    };
    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [moreOpen]);

  const currentTitle = useMemo(() => resolveRouteTitle(location.pathname, t, location.search), [location.pathname, location.search, t]);
  const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
  const currentWorkspace = useMemo(
    () =>
      resolveActiveWorkspace({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspace,
      }),
    [user, location.pathname, activeWorkspace, preferredWorkspace]
  );
  const currentWorkspaceLabel = currentWorkspace ? t(WORKSPACE_META[currentWorkspace].labelKey) : '--';
  const canUseAssistant = currentWorkspace === 'STUDENT_WORKSPACE' && userHasCapability(user, 'STUDENT_WORKSPACE');
  const workspaceHomePath = homePathForWorkspace(currentWorkspace);
  const isWorkspaceHome = location.pathname === workspaceHomePath;

  return (
    <header className="safe-area-top sticky top-0 z-40 overflow-x-clip border-b border-border-subtle bg-surface/95 px-3 pb-2.5 sm:px-4 sm:pb-3 lg:px-8">
      <div className="mx-auto flex min-h-14 w-full max-w-[1480px] items-center justify-between gap-2 sm:gap-4">
        <div className="relative z-10 flex min-w-0 flex-1 items-center gap-2 sm:gap-3">
        <button
          type="button"
          aria-label={t('common.actions.openNavigation')}
          onClick={openMobileSidebar}
          className="flex min-h-11 min-w-11 shrink-0 items-center justify-center rounded-lg border border-border-subtle p-2 lg:hidden"
        >
          <Menu size={18} className="text-slate-500 dark:text-white/70" aria-hidden="true" />
        </button>
        {!isWorkspaceHome && (
          <button
            type="button"
            aria-label={t('common.actions.backToWorkspace')}
            title={t('common.actions.backToWorkspace')}
            onClick={() => navigate(workspaceHomePath)}
            className="flex min-h-11 min-w-11 shrink-0 items-center justify-center rounded-lg border border-border-subtle text-slate-500 hover:border-border-strong hover:bg-surface-sunken dark:text-white/70 lg:hidden"
          >
            <ChevronLeft size={18} aria-hidden="true" />
          </button>
        )}
        <div className="min-w-0 flex-1">
          <div className="type-metadata mb-1 truncate text-[0.625rem] sm:text-[0.6875rem]">
            {currentWorkspaceLabel}
          </div>
          {!isWorkspaceHome ? (
            <div className="type-section-title truncate text-slate-900 dark:text-white">{currentTitle}</div>
          ) : null}
        </div>
        {canUseAssistant && (
          <div className="group relative hidden min-w-0 max-w-xl flex-1 lg:block">
            <Search
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30 group-focus-within:text-[hsl(var(--ai))] transition-colors duration-300"
              size={16}
            />
            <input
              aria-label={t('shell.drawerPromptLabel')}
              type="text"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setAssistantDraft(event.target.value);
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  openAssistant(search.trim());
                }
              }}
              placeholder={t('shell.searchPlaceholder')}
              className="surface-control w-full rounded-lg py-2.5 pl-10 pr-4 text-sm focus:border-[hsl(var(--ai)/0.6)] focus:outline-none"
            />
          </div>
        )}
      </div>
        <div className="relative z-10 flex shrink-0 items-center gap-1.5 sm:gap-2.5">
        {canUseAssistant && (
          <button
            type="button"
            aria-label={t('common.actions.openAssistant')}
            onClick={() => openAssistant(search.trim())}
            className="flex min-h-11 min-w-11 items-center justify-center gap-2 rounded-lg border border-[hsl(var(--ai)/0.28)] px-2.5 py-2 text-xs font-semibold text-[hsl(var(--ai))] hover:border-[hsl(var(--ai)/0.5)] hover:bg-[hsl(var(--ai)/0.08)] dark:text-[hsl(var(--ai))] sm:min-w-0 sm:px-3"
          >
            <Brain size={14} aria-hidden="true" />
            <span className="hidden lg:inline">{t('common.actions.openAssistant')}</span>
          </button>
        )}
        <NotificationBell />
        <button
          type="button"
          aria-label={t('common.localeLabel')}
          onClick={() => setLocale(locale === 'zh-CN' ? 'en-US' : 'zh-CN')}
          className="hidden min-h-11 items-center gap-2 rounded-lg border border-border-subtle px-3 py-2 text-xs font-semibold sm:flex"
        >
          {locale === 'zh-CN' ? 'EN' : '中'}
        </button>
        <button
          type="button"
          aria-label={isDarkMode ? t('common.actions.lightMode') : t('common.actions.darkMode')}
          title={isDarkMode ? t('common.actions.lightMode') : t('common.actions.darkMode')}
          onClick={toggleDarkMode}
          className="hidden min-h-11 min-w-11 items-center justify-center rounded-lg border border-border-subtle p-2.5 hover:border-border-strong hover:bg-surface-sunken sm:flex"
        >
          {isDarkMode ? <Sun size={18} className="text-amber-400" aria-hidden="true" /> : <Moon size={18} className="text-slate-500" aria-hidden="true" />}
        </button>
        <div ref={moreMenuRef} className="relative sm:hidden">
          <button
            type="button"
            aria-label={t('common.actions.moreOptions')}
            aria-haspopup="menu"
            aria-expanded={moreOpen}
            onClick={() => setMoreOpen((open) => !open)}
            className="flex min-h-11 min-w-11 items-center justify-center rounded-lg border border-border-subtle p-2.5 hover:border-border-strong hover:bg-surface-sunken"
          >
            <MoreVertical size={18} className="text-slate-500 dark:text-white/70" aria-hidden="true" />
          </button>
          {moreOpen ? (
            <div
              role="menu"
              className="absolute right-0 z-[50] mt-2 w-44 overflow-hidden rounded-xl border border-border-subtle bg-surface p-1 shadow-md"
            >
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setLocale(locale === 'zh-CN' ? 'en-US' : 'zh-CN');
                  setMoreOpen(false);
                }}
                className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-semibold text-slate-700 hover:bg-surface-sunken dark:text-white/80"
              >
                {locale === 'zh-CN' ? 'English' : '中文'}
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  toggleDarkMode();
                  setMoreOpen(false);
                }}
                className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-semibold text-slate-700 hover:bg-surface-sunken dark:text-white/80"
              >
                {isDarkMode ? (
                  <>
                    <Sun size={16} className="text-amber-400" aria-hidden="true" />
                    {t('common.actions.lightMode')}
                  </>
                ) : (
                  <>
                    <Moon size={16} className="text-slate-500" aria-hidden="true" />
                    {t('common.actions.darkMode')}
                  </>
                )}
              </button>
            </div>
          ) : null}
        </div>
        {user && (
          <Link
            to="/settings"
            aria-label={`${t('shell.nav.settings')}: ${user.displayName}`}
            title={user.displayName}
            className="flex min-h-11 min-w-11 items-center justify-center rounded-lg border border-border-subtle text-primary hover:border-primary/40 hover:bg-primary/5 lg:hidden"
          >
            <UserRound size={18} aria-hidden="true" />
          </Link>
        )}
        {user && (
          <div className="hidden max-w-44 items-center gap-2 border-l border-border-subtle pl-3 lg:flex">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
              <UserRound size={16} aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-slate-900 dark:text-white">{user.displayName}</div>
              <div className="truncate text-[10px] uppercase tracking-[0.12em] text-slate-400 dark:text-white/35">{user.username}</div>
            </div>
          </div>
        )}
        </div>
      </div>
    </header>
  );
};

export const AppLayout: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const closeMobileSidebar = useUIStore((state) => state.closeMobileSidebar);

  useEffect(() => {
    closeMobileSidebar();
  }, [closeMobileSidebar, location.pathname]);

  return (
    <div className="min-h-screen min-w-0 overflow-x-clip flex bg-background">
      <Sidebar />
      <MobileSidebarDrawer />
      <div className="flex-1 flex flex-col min-w-0">
        <Topbar />
        <main className="safe-area-bottom relative z-10 flex-1 min-w-0 px-3 py-5 sm:px-4 sm:py-6 lg:px-8 lg:py-8">
          <RouteErrorBoundary
            title={t('common.errors.routeTitle')}
            description={t('common.errors.routeDescription')}
          >
            <div className="mx-auto w-full max-w-[1480px]">
              <Outlet />
            </div>
          </RouteErrorBoundary>
        </main>
      </div>
      <AssistantDrawer />
    </div>
  );
};
