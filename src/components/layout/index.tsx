import React, { useEffect, useMemo, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Activity,
  BookCopy,
  BookOpen,
  Brain,
  ChevronLeft,
  Database,
  GraduationCap,
  LayoutDashboard,
  LineChart,
  LogOut,
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
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { Magnetic } from '@/components/common';
import { useAuthStore, useUIStore } from '@/store';
import { aiService } from '@/lib/services';
import { roleHomePath } from '@/lib/format';
import type { Role } from '@/lib/contracts';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

type NavItem = {
  name: string;
  path: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
};

function buildSections(role?: Role | null): Array<{ label: string; items: NavItem[] }> {
  if (role === 'TEACHER') {
    return [
      {
        label: 'Teaching',
        items: [
          { name: '班级总览', path: '/teacher/classes', icon: Users },
          { name: '诊断模板', path: '/teacher/diagnosis-templates', icon: Brain },
          { name: '词对管理', path: '/teacher/lexical-pairs', icon: BookOpen },
          { name: '词表管理', path: '/teacher/lexical-lists', icon: BookCopy },
          { name: '干预工作台', path: '/teacher/interventions', icon: Shield },
        ],
      },
      {
        label: 'System',
        items: [{ name: '设置', path: '/settings', icon: Settings }],
      },
    ];
  }
  if (role === 'ADMIN') {
    return [
      {
        label: 'Admin',
        items: [{ name: '用户管理', path: '/admin/users', icon: Users }],
      },
      {
        label: 'System',
        items: [{ name: '设置', path: '/settings', icon: Settings }],
      },
    ];
  }
  return [
    {
      label: 'Core',
      items: [
        { name: '总览', path: '/dashboard', icon: LayoutDashboard },
        { name: '智能诊断', path: '/diagnosis', icon: Activity },
        { name: '个性化训练', path: '/training', icon: GraduationCap },
        { name: '学情分析', path: '/analytics', icon: LineChart },
        { name: '错题与复习', path: '/errors', icon: Database },
      ],
    },
    {
      label: 'System',
      items: [{ name: '设置', path: '/settings', icon: Settings }],
    },
  ];
}

function roleLabel(role?: Role | null): string {
  if (role === 'TEACHER') {
    return 'Teacher';
  }
  if (role === 'ADMIN') {
    return 'Administrator';
  }
  return 'Student';
}

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { isSidebarCollapsed, toggleSidebar } = useUIStore();
  const sections = useMemo(() => buildSections(user?.primaryRole), [user?.primaryRole]);

  return (
    <aside
      className={cn(
        'sidebar-shell h-[calc(100vh-1.5rem)] my-3 ml-3 flex flex-col transition-all duration-700 z-50 liquid-glass-panel rounded-3xl edge-light fluid-texture',
        isSidebarCollapsed ? 'w-20' : 'w-72'
      )}
    >
      <div className="p-8 flex items-center justify-between relative z-10">
        {!isSidebarCollapsed && (
          <Link to={roleHomePath(user?.primaryRole)} className="flex items-center gap-3">
            <div className="relative w-9 h-9 flex items-center justify-center">
              <div className="absolute inset-0 bg-primary/15 rounded-xl rotate-6" />
              <div className="absolute inset-0 border border-primary/40 rounded-xl -rotate-6" />
              <Sparkles size={16} className="text-primary relative z-10" />
            </div>
            <div className="flex flex-col">
              <span className="text-lg font-black tracking-tight text-slate-900 dark:text-white leading-none">
                EF<span className="text-primary">.</span>Transfer
              </span>
              <span className="text-[10px] font-bold tracking-[0.24em] uppercase text-slate-400 dark:text-white/30 leading-none mt-1">
                learning workspace
              </span>
            </div>
          </Link>
        )}
        <Magnetic strength={0.18}>
          <button
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
      </div>

      <nav className="flex-1 px-4 py-4 overflow-y-auto no-scrollbar relative z-10 space-y-8">
        {sections.map((section) => (
          <div key={section.label} className="space-y-2">
            {!isSidebarCollapsed && (
              <h4 className="px-4 text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/20">
                {section.label}
              </h4>
            )}
            <div className="space-y-1">
              {section.items.map((item) => (
                <motion.div key={item.path} whileHover={{ x: 4 }} whileTap={{ scale: 0.98 }}>
                  <NavLink
                    to={item.path}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-3 px-4 py-3 rounded-2xl transition-all duration-300 group relative',
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
                        {!isSidebarCollapsed && <span className="relative z-10 tracking-wide text-xs">{item.name}</span>}
                      </>
                    )}
                  </NavLink>
                </motion.div>
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-4 relative z-10 mt-auto border-t border-white/5 space-y-4">
        {!isSidebarCollapsed && user && (
          <div className="px-4 py-3 rounded-2xl bg-white/40 dark:bg-white/5 border border-slate-200 dark:border-white/5">
            <div className="text-sm font-black text-slate-900 dark:text-white/90">{user.displayName}</div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mt-1">
              {roleLabel(user.primaryRole)}
            </div>
          </div>
        )}
        <Magnetic strength={0.1}>
          <button
            onClick={() => void logout()}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-rose-500 dark:text-rose-400 hover:bg-rose-500/10 transition-all border border-transparent hover:border-rose-500/20 font-bold"
          >
            <LogOut size={18} />
            {!isSidebarCollapsed && <span className="uppercase tracking-widest text-[9px]">Sign Out</span>}
          </button>
        </Magnetic>
      </div>
    </aside>
  );
};

const AssistantDrawer: React.FC = () => {
  const { user } = useAuthStore();
  const { isAssistantOpen, assistantDraft, closeAssistant, setAssistantDraft } = useUIStore();
  const [query, setQuery] = useState(assistantDraft);
  const ragMutation = useMutation({
    mutationFn: (value: string) => aiService.queryLexicalRag(value),
  });

  useEffect(() => {
    setQuery(assistantDraft);
  }, [assistantDraft]);

  if (user?.primaryRole !== 'STUDENT') {
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
      {isAssistantOpen && (
        <>
          <motion.button
            type="button"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={closeAssistant}
            className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-[70]"
          />
          <motion.aside
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', stiffness: 260, damping: 28 }}
            className="fixed top-0 right-0 h-screen w-full max-w-xl liquid-glass-panel z-[80] border-l border-white/10 p-6 overflow-y-auto"
          >
            <div className="flex items-center justify-between mb-6">
              <div>
                <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-2">Lexical RAG</div>
                <h3 className="text-2xl font-black text-slate-900 dark:text-white">词汇检索助手</h3>
              </div>
              <button
                type="button"
                onClick={closeAssistant}
                className="p-3 rounded-2xl border border-slate-200 dark:border-white/10 hover:bg-black/5 dark:hover:bg-white/5"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={submit} className="space-y-4">
              <textarea
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                rows={4}
                placeholder="输入你想追问的词汇迁移问题，例如：coin / coin 为什么容易误判？"
                className="w-full rounded-3xl bg-white/70 dark:bg-slate-950/50 border border-slate-200 dark:border-white/10 px-5 py-4 outline-none focus:border-primary/50"
              />
              <button type="submit" className="btn-liquid px-6 py-3 text-white">
                开始检索
              </button>
            </form>

            {ragMutation.isPending && (
              <div className="mt-6 rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                正在检索知识片段并生成解释...
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
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">Answer</div>
                    {ragMutation.data.fallbackReason && (
                      <span className="text-[10px] uppercase tracking-[0.24em] text-amber-500">规则回退</span>
                    )}
                  </div>
                  <p className="text-base leading-7 text-slate-800 dark:text-white/85">{ragMutation.data.answer}</p>
                  <p className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/50">{ragMutation.data.explanation}</p>
                </section>

                <section className="rounded-3xl border border-slate-200 dark:border-white/10 p-6 bg-white/50 dark:bg-white/5">
                  <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">Actions</div>
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
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">Citations</div>
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
                    <div className="text-xs uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-4">Context</div>
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
  const location = useLocation();
  const { user } = useAuthStore();
  const { isDarkMode, toggleDarkMode, openAssistant, assistantDraft, setAssistantDraft } = useUIStore();
  const [search, setSearch] = useState(assistantDraft);

  useEffect(() => {
    setSearch(assistantDraft);
  }, [assistantDraft]);

  const currentTitle = useMemo(() => {
    const path = location.pathname;
    if (path.startsWith('/teacher/classes')) {
      return '教师工作台';
    }
    if (path.startsWith('/teacher/diagnosis-templates')) {
      return '诊断模板';
    }
    if (path.startsWith('/teacher/lexical-pairs')) {
      return '词对管理';
    }
    if (path.startsWith('/teacher/lexical-lists')) {
      return '词表管理';
    }
    if (path.startsWith('/teacher/interventions')) {
      return '干预工作台';
    }
    if (path.startsWith('/admin/users')) {
      return '用户管理';
    }
    if (path.startsWith('/diagnosis')) {
      return '智能诊断';
    }
    if (path.startsWith('/training')) {
      return '个性化训练';
    }
    if (path.startsWith('/analytics')) {
      return '学情分析';
    }
    if (path.startsWith('/errors')) {
      return '错题与复习';
    }
    return '学习总览';
  }, [location.pathname]);

  return (
    <header className="h-16 mt-3 mx-4 lg:mx-8 flex items-center justify-between px-6 sticky top-3 z-40 liquid-glass rounded-2xl edge-light fluid-texture">
      <div className="flex items-center gap-5 flex-1 relative z-10">
        <div>
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-1">
            {roleLabel(user?.primaryRole)}
          </div>
          <div className="text-lg font-black text-slate-900 dark:text-white">{currentTitle}</div>
        </div>
        {user?.primaryRole === 'STUDENT' && (
          <div className="relative max-w-lg w-full hidden md:block group">
            <Search
              className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30 group-focus-within:text-primary transition-colors duration-300"
              size={16}
            />
            <input
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
              placeholder="追问词汇迁移问题，回车打开助手"
              className="w-full bg-white/50 dark:bg-black/30 border border-slate-200 dark:border-white/5 focus:border-primary/40 rounded-full py-2.5 pl-11 pr-4 text-sm transition-all focus:outline-none"
            />
          </div>
        )}
      </div>
      <div className="flex items-center gap-5 relative z-10">
        {user?.primaryRole === 'STUDENT' && (
          <button
            type="button"
            onClick={() => openAssistant(search.trim())}
            className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-full border border-slate-200 dark:border-white/10 hover:border-primary/40 text-xs font-black uppercase tracking-[0.2em]"
          >
            <Brain size={14} />
            AI 助手
          </button>
        )}
        <Magnetic strength={0.16}>
          <button
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
  return (
    <div className="min-h-screen flex bg-background">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Topbar />
        <main className="px-4 lg:px-8 py-8 flex-1 relative z-10">
          <Outlet />
        </main>
      </div>
      <AssistantDrawer />
    </div>
  );
};
