import React, { useEffect, useMemo, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Activity,
  BookCopy,
  BookOpen,
  Brain,
  FilePenLine,
  ChevronLeft,
  Database,
  GraduationCap,
  History,
  LayoutDashboard,
  LineChart,
  LogOut,
  Menu,
  Moon,
  Search,
  Settings,
  Shield,
  Sparkles,
  Sun,
  Users,
  X,
} from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Magnetic } from '@/components/common';
import { RouteErrorBoundary } from '@/components/common/AppErrorBoundary';
import { NotificationBell } from './NotificationBell';
import { useAuthStore, useUIStore } from '@/store';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
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
  icon: React.ComponentType<{ size?: number; className?: string }>;
};

type WorkspaceMeta = {
  labelKey: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
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
    <div className={cn(isCollapsed ? 'px-3 pt-1 pb-2' : 'px-4 pt-1 pb-2')}>
      {!isCollapsed && (
        <div className="px-4 text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/20">
          {t('shell.switchWorkspaceLabel')}
        </div>
      )}
      <div
        className={cn(
          'mt-3 rounded-[1.6rem] border border-slate-200/80 bg-white/45 dark:border-white/10 dark:bg-white/5',
          isCollapsed ? 'p-2 space-y-2' : 'p-2.5 space-y-2'
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
              onClick={() => onSelect(workspace)}
              className={cn(
                'w-full rounded-2xl border transition-all duration-300',
                isCollapsed ? 'flex items-center justify-center px-3 py-3' : 'flex items-center gap-3 px-4 py-3 text-left',
                isActive
                  ? 'border-primary/20 bg-primary/[0.08] text-primary shadow-[0_0_20px_rgba(59,130,246,0.12)]'
                  : 'border-transparent text-slate-500 hover:border-slate-200 hover:text-slate-900 dark:text-white/55 dark:hover:border-white/10 dark:hover:text-white'
              )}
            >
              <meta.icon size={16} />
              {!isCollapsed && (
                <div className="min-w-0">
                  <div className="text-xs font-black tracking-wide">{t(meta.labelKey)}</div>
                  <div className="mt-1 text-[10px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
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
          'relative z-10',
          isCollapsed ? 'px-3 pb-3 pt-4 flex flex-col items-center gap-3' : 'p-8 flex items-center justify-between gap-3'
        )}
      >
        <Link
          to={homePath}
          className={cn('flex items-center', isCollapsed ? 'justify-center' : 'gap-3')}
          aria-label={t('common.appName')}
          onClick={onNavigate}
        >
          <div className="relative w-9 h-9 flex items-center justify-center">
            <div className="absolute inset-0 bg-primary/15 rounded-xl rotate-6" />
            <div className="absolute inset-0 border border-primary/40 rounded-xl -rotate-6" />
            <Sparkles size={16} className="text-primary relative z-10" />
          </div>
          {!isCollapsed && (
            <div className="flex flex-col">
              <span className="text-lg font-black tracking-tight text-slate-900 dark:text-white leading-none">
                EF<span className="text-primary">.</span>Transfer
              </span>
              <span className="mt-1 text-[11px] font-bold text-slate-400 dark:text-white/30 leading-none">
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
          'flex-1 overflow-y-auto no-scrollbar relative z-10',
          isCollapsed ? 'px-3 py-3 space-y-4' : 'px-4 py-4 space-y-8'
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
          <div key={section.label} className={cn(isCollapsed ? 'space-y-1.5' : 'space-y-2')}>
            {!isCollapsed && (
              <h4 className="px-4 text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/20">
                {section.label}
              </h4>
            )}
            <div className="space-y-1">
              {section.items.map((item) => (
                <motion.div key={item.path} whileHover={isCollapsed ? undefined : { x: 4 }} whileTap={{ scale: 0.98 }}>
                  <NavLink
                    to={item.path}
                    aria-label={item.name}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      cn(
                        'flex rounded-2xl transition-all duration-300 group relative',
                        isCollapsed ? 'items-center justify-center px-3 py-3.5' : 'items-center gap-3 px-4 py-3',
                        isActive ? 'text-primary font-black' : 'text-slate-500 dark:text-white/50 hover:text-primary dark:hover:text-white'
                      )
                    }
                  >
                    {({ isActive }) => (
                      <>
                        {isActive && (
                          <motion.div
                            layoutId="active-pill"
                            className="absolute inset-0 bg-primary/[0.08] border border-primary/20 rounded-2xl shadow-[0_0_20px_rgba(59,130,246,0.12)]"
                            transition={{ type: 'spring', stiffness: 350, damping: 25 }}
                          />
                        )}
                        <item.icon size={18} className="relative z-10" />
                        {!isCollapsed && <span className="relative z-10 tracking-wide text-xs">{item.name}</span>}
                      </>
                    )}
                  </NavLink>
                </motion.div>
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div
        className={cn(
          'relative z-10 mt-auto border-t border-white/5',
          isCollapsed ? 'p-3' : 'p-4 space-y-4'
        )}
      >
        {!isCollapsed && user && (
            <div className="px-4 py-3 rounded-2xl bg-white/40 dark:bg-white/5 border border-slate-200 dark:border-white/5">
              <div className="text-sm font-black text-slate-900 dark:text-white/90">{user.displayName}</div>
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mt-1">
                {roleLabels.join(' / ')}
              </div>
            </div>
          )}
        <div className={cn(isCollapsed && 'flex justify-center')}>
          <Magnetic strength={0.1}>
            <button
              type="button"
              aria-label={t('common.actions.signOut')}
              onClick={() => {
                onNavigate?.();
                void logout();
              }}
              className={cn(
                'flex items-center rounded-2xl text-rose-500 dark:text-rose-400 hover:bg-rose-500/10 transition-all border border-transparent hover:border-rose-500/20 font-bold',
                isCollapsed ? 'justify-center p-3' : 'w-full gap-3 px-4 py-3'
              )}
            >
              <LogOut size={18} />
              {!isCollapsed && <span className="uppercase tracking-widest text-[9px]">{t('common.actions.signOut')}</span>}
            </button>
          </Magnetic>
        </div>
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
        'sidebar-shell hidden lg:flex h-[calc(100vh-1.5rem)] my-3 ml-3 flex-col transition-all duration-700 z-50 liquid-glass-panel rounded-3xl edge-light fluid-texture',
        isSidebarCollapsed ? 'w-20' : 'w-72'
      )}
    >
      <SidebarContent
        isCollapsed={isSidebarCollapsed}
        navigationLabel={t('shell.mobileNavigationTitle')}
        headerAction={
          <Magnetic strength={0.18}>
            <button
              type="button"
              aria-label={isSidebarCollapsed ? t('common.actions.expandSidebar') : t('common.actions.collapseSidebar')}
              onClick={toggleSidebar}
              className="p-2.5 hover:bg-black/5 dark:hover:bg-white/10 rounded-xl transition-colors backdrop-blur-md border border-slate-200 dark:border-white/5"
            >
              <ChevronLeft
                className={cn(
                  'transition-transform duration-500 text-slate-400 dark:text-white/70',
                  isSidebarCollapsed && 'rotate-180'
                )}
                size={20}
              />
            </button>
          </Magnetic>
        }
      />
    </aside>
  );
};

const MobileSidebarDrawer: React.FC = () => {
  const { t } = useTranslation();
  const { isMobileSidebarOpen, closeMobileSidebar } = useUIStore();
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

  return (
    <AnimatePresence>
      {isMobileSidebarOpen && (
        <>
          <motion.div
            aria-hidden="true"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeMobileSidebar}
            className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-[70] lg:hidden"
          />
          <motion.aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="mobile-sidebar-title"
            tabIndex={-1}
            initial={{ x: '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: '-100%' }}
            transition={{ type: 'spring', stiffness: 260, damping: 28 }}
            className="sidebar-shell fixed inset-y-3 left-3 z-[80] flex w-[min(22rem,calc(100vw-1.5rem))] flex-col liquid-glass-panel rounded-3xl edge-light fluid-texture lg:hidden"
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
                  className="p-2.5 hover:bg-black/5 dark:hover:bg-white/10 rounded-xl transition-colors backdrop-blur-md border border-slate-200 dark:border-white/5"
                >
                  <X size={20} className="text-slate-400 dark:text-white/70" />
                </button>
              }
            />
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
};

const AssistantDrawer: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const { user } = useAuthStore();
  const { activeWorkspace, preferredWorkspaceByUser, isAssistantOpen, assistantDraft, closeAssistant, setAssistantDraft } = useUIStore();
  const [query, setQuery] = useState(assistantDraft);
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
  const ragMutation = useMutation({
    mutationFn: (value: string) => aiService.queryLexicalRag(value),
  });

  useEffect(() => {
    setQuery(assistantDraft);
  }, [assistantDraft]);

  useEffect(() => {
    if (!canUseAssistant && isAssistantOpen) {
      closeAssistant();
    }
  }, [canUseAssistant, isAssistantOpen, closeAssistant]);

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

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) {
      return;
    }
    setAssistantDraft(trimmed);
    ragMutation.mutate(trimmed);
  };

  return (
    <AnimatePresence>
      {assistantOpen && (
        <>
          <motion.div
            aria-hidden="true"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeAssistant}
            className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-[70]"
          />
          <motion.aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="assistant-drawer-title"
            tabIndex={-1}
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', stiffness: 260, damping: 28 }}
            className="fixed top-0 right-0 h-screen w-full max-w-xl liquid-glass-panel z-[80] border-l border-white/10 p-6 overflow-y-auto"
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

            <form onSubmit={submit} className="space-y-4">
              <textarea
                aria-label={t('shell.drawerPromptLabel')}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                rows={4}
                placeholder={t('shell.drawerPromptPlaceholder')}
                className="w-full rounded-3xl bg-white/70 dark:bg-slate-950/50 border border-slate-200 dark:border-white/10 px-5 py-4 outline-none focus:border-primary/50"
              />
              <button type="submit" className="btn-liquid px-6 py-3 text-white">
                {t('common.actions.search')}
              </button>
            </form>

            {ragMutation.isPending && (
              <div className="mt-6 rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                {t('common.loading.searchingKnowledge')}
              </div>
            )}

            {ragMutation.error && (
              <div className="mt-6 rounded-3xl border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
                {ragMutation.error.message}
              </div>
            )}

            {ragMutation.data && (
              <div className="mt-6 space-y-5">
                <section className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                  <div className="flex items-center justify-between gap-3 mb-4">
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('shell.answer')}</div>
                    {ragMutation.data.fallbackReason && (
                      <span className="text-[10px] uppercase tracking-[0.24em] text-amber-500">{t('shell.fallbackReason')}</span>
                    )}
                  </div>
                  <p className="text-base leading-7 text-slate-800 dark:text-white/85">{ragMutation.data.answer}</p>
                  <p className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/50">{ragMutation.data.explanation}</p>
                </section>

                <section className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                  <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">{t('shell.actions')}</div>
                  <div className="space-y-3">
                    {ragMutation.data.recommendedActions.map((item) => (
                      <div key={item} className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-3">
                        {item}
                      </div>
                    ))}
                  </div>
                </section>

                {!!ragMutation.data.citations?.length && (
                  <section className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">{t('shell.citations')}</div>
                    <div className="space-y-3">
                      {ragMutation.data.citations.map((citation) => (
                        <div key={citation.citationId} className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-3">
                          <div className="font-bold text-slate-900 dark:text-white/90">{citation.title || citation.sourceId}</div>
                          <div className="text-sm text-slate-500 dark:text-white/50 mt-1">{citation.snippet}</div>
                        </div>
                      ))}
                    </div>
                  </section>
                )}

                {!!ragMutation.data.contextChunks?.length && (
                  <section className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">{t('shell.context')}</div>
                    <div className="space-y-3">
                      {ragMutation.data.contextChunks.map((chunk) => (
                        <details key={`${chunk.citationId}-${chunk.sourceId}`} className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-3">
                          <summary className="cursor-pointer font-bold text-slate-900 dark:text-white/90">
                            {chunk.title || chunk.sourceId}
                          </summary>
                          <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-500 dark:text-white/55">
                            {chunk.content || chunk.snippet}
                          </p>
                        </details>
                      ))}
                    </div>
                  </section>
                )}
              </div>
            )}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
};

export const Topbar: React.FC = () => {
  const { t } = useTranslation();
  const location = useLocation();
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

  useEffect(() => {
    setSearch(assistantDraft);
  }, [assistantDraft]);

  const currentTitle = useMemo(() => resolveRouteTitle(location.pathname, t), [location.pathname, t]);
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

  return (
    <header className="h-16 mt-3 mx-4 lg:mx-8 flex items-center justify-between px-6 sticky top-3 z-40 liquid-glass rounded-2xl edge-light fluid-texture">
      <div className="flex items-center gap-5 flex-1 relative z-10">
        <button
          type="button"
          aria-label={t('common.actions.openNavigation')}
          onClick={openMobileSidebar}
          className="flex lg:hidden items-center justify-center rounded-full border border-slate-200 dark:border-white/10 p-2.5 hover:border-primary/40"
        >
          <Menu size={18} className="text-slate-500 dark:text-white/70" />
        </button>
        <div>
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-1">
            {currentWorkspaceLabel}
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-white">{currentTitle}</div>
        </div>
        {canUseAssistant && (
          <div className="relative max-w-lg w-full hidden md:block group">
            <Search
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30 group-focus-within:text-primary transition-colors duration-300"
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
              className="w-full bg-white/50 dark:bg-black/30 border border-slate-200 dark:border-white/5 focus:border-primary/40 rounded-full py-2.5 pl-11 pr-4 text-sm transition-all focus:outline-none"
            />
          </div>
        )}
      </div>
      <div className="flex items-center gap-5 relative z-10">
        {canUseAssistant && (
          <button
            type="button"
            aria-label={t('common.actions.openAssistant')}
            onClick={() => openAssistant(search.trim())}
            className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-full border border-slate-200 dark:border-white/10 hover:border-primary/40 text-xs font-black uppercase tracking-[0.2em]"
          >
            <Brain size={14} />
            {t('common.actions.openAssistant')}
          </button>
        )}
        <NotificationBell />
        <button
          type="button"
          aria-label={t('common.localeLabel')}
          onClick={() => setLocale(locale === 'zh-CN' ? 'en-US' : 'zh-CN')}
          className="hidden sm:flex items-center gap-2 px-3 py-2 rounded-full border border-slate-200 dark:border-white/10 text-xs font-black uppercase tracking-[0.2em]"
        >
          {locale === 'zh-CN' ? 'EN' : '中'}
        </button>
        <Magnetic strength={0.16}>
          <button
            type="button"
            aria-label={isDarkMode ? t('common.actions.lightMode') : t('common.actions.darkMode')}
            onClick={toggleDarkMode}
            className="p-2.5 hover:bg-black/5 dark:hover:bg-white/5 rounded-full transition-all group border border-transparent hover:border-slate-200 dark:hover:border-white/10"
          >
            {isDarkMode ? <Sun size={20} className="text-amber-400" /> : <Moon size={20} className="text-slate-400" />}
          </button>
        </Magnetic>
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
    <div className="min-h-screen flex bg-background">
      <Sidebar />
      <MobileSidebarDrawer />
      <div className="flex-1 flex flex-col min-w-0">
        <Topbar />
        <main className="px-4 lg:px-8 py-8 flex-1 relative z-10">
          <RouteErrorBoundary
            title={t('common.errors.routeTitle')}
            description={t('common.errors.routeDescription')}
          >
            <Outlet />
          </RouteErrorBoundary>
        </main>
      </div>
      <AssistantDrawer />
    </div>
  );
};
