import React from 'react';
import { Link, useLocation, Outlet } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Activity, 
  GraduationCap, 
  LineChart, 
  Settings, 
  LogOut, 
  ChevronLeft,
  Sun,
  Moon,
  Bell,
  Search,
  BookOpen
} from 'lucide-react';
import { useAuthStore, useUIStore } from '@/store';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 1. 侧边栏组件
export const Sidebar: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { isSidebarCollapsed, toggleSidebar } = useUIStore();
  const location = useLocation();

  const menuItems = user?.role === 'STUDENT' ? [
    { name: '总览', icon: LayoutDashboard, path: '/dashboard' },
    { name: '智能诊断', icon: Activity, path: '/diagnosis' },
    { name: '个性化训练', icon: GraduationCap, path: '/training' },
    { name: '学情分析', icon: LineChart, path: '/analytics' },
  ] : [
    { name: '班级管理', icon: BookOpen, path: '/teacher' },
  ];

  return (
    <aside className={cn(
      "h-[calc(100vh-1.5rem)] my-3 ml-3 flex flex-col transition-all duration-300 z-50 liquid-glass-panel rounded-3xl edge-light fluid-texture",
      isSidebarCollapsed ? "w-20" : "w-64"
    )}>
      <div className="p-6 flex items-center justify-between relative z-10">
        {!isSidebarCollapsed && (
          <h1 className="text-xl font-black bg-gradient-to-r from-primary via-purple-400 to-accent bg-clip-text text-transparent drop-shadow-[0_0_10px_rgba(139,92,246,0.5)]">
            EF-Transfer
          </h1>
        )}
        <button onClick={toggleSidebar} className="p-2 hover:bg-white/10 rounded-xl transition-colors backdrop-blur-md border border-white/5">
          <ChevronLeft className={cn("transition-transform", isSidebarCollapsed && "rotate-180")} size={20} />
        </button>
      </div>
      <nav className="flex-1 px-4 space-y-2 overflow-y-auto no-scrollbar relative z-10 mt-4">
        {menuItems.map((item) => (
          <Link key={item.path} to={item.path} className={cn(
            "flex items-center gap-3 px-4 py-3 rounded-2xl transition-all group relative overflow-hidden",
            location.pathname === item.path 
              ? "bg-primary/20 text-primary font-bold shadow-[0_0_15px_rgba(139,92,246,0.3)] border border-primary/30" 
              : "text-muted-foreground hover:bg-white/5 hover:text-foreground border border-transparent"
          )}>
            {location.pathname === item.path && (
              <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-primary rounded-r-full shadow-[0_0_10px_rgba(139,92,246,0.8)]" />
            )}
            <item.icon size={20} className={cn(
              "transition-transform group-hover:scale-110",
              location.pathname === item.path && "drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]"
            )} />
            {!isSidebarCollapsed && <span>{item.name}</span>}
          </Link>
        ))}
      </nav>
      <div className="p-4 relative z-10 mt-auto">
        <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 hover:shadow-[0_0_15px_rgba(244,63,94,0.2)] hover:border-rose-500/20 border border-transparent transition-all">
          <LogOut size={20} />
          {!isSidebarCollapsed && <span className="font-medium">退出系统</span>}
        </button>
      </div>
    </aside>
  );
};

// 2. 顶栏组件
export const Topbar: React.FC = () => {
  const { user } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();

  return (
    <header className="h-16 mt-3 mx-4 lg:mx-8 flex items-center justify-between px-6 sticky top-3 z-40 liquid-glass rounded-2xl edge-light fluid-texture">
      <div className="flex items-center gap-4 flex-1 relative z-10">
        <div className="relative max-w-md w-full hidden md:block group">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground group-focus-within:text-primary transition-colors" size={16} />
          <input 
            type="text" 
            placeholder="Search analytics, tasks..." 
            className="w-full bg-black/20 border border-white/5 focus:border-primary/50 focus:bg-black/40 rounded-full py-2 pl-11 pr-4 text-sm transition-all focus:outline-none focus:ring-2 focus:ring-primary/20 text-foreground placeholder:text-muted-foreground" 
          />
        </div>
      </div>
      <div className="flex items-center gap-4 relative z-10">
        <button className="p-2 hover:bg-white/10 rounded-full transition-colors relative group">
          <Bell size={20} className="text-muted-foreground group-hover:text-foreground" />
          <div className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(244,63,94,0.8)]" />
        </button>
        <button onClick={toggleDarkMode} className="p-2 hover:bg-white/10 rounded-full transition-colors group">
          {isDarkMode ? (
            <Sun size={20} className="text-muted-foreground group-hover:text-amber-400 drop-shadow-none group-hover:drop-shadow-[0_0_8px_rgba(251,191,36,0.6)]" />
          ) : (
            <Moon size={20} className="text-muted-foreground group-hover:text-blue-400 drop-shadow-none group-hover:drop-shadow-[0_0_8px_rgba(96,165,250,0.6)]" />
          )}
        </button>
        <div className="w-px h-6 bg-white/10 mx-1" />
        <div className="flex items-center gap-3 pl-1 cursor-pointer group">
          <div className="text-right hidden sm:block">
            <p className="text-sm font-bold leading-tight group-hover:text-primary transition-colors">{user?.username || 'Guest'}</p>
            <p className="text-[10px] text-muted-foreground uppercase tracking-wider">{user?.role || 'USER'}</p>
          </div>
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-white font-bold shadow-[0_0_15px_rgba(139,92,246,0.4)] border border-white/20">
            {user?.username?.[0] || 'G'}
          </div>
        </div>
      </div>
    </header>
  );
};

// 3. 布局容器组件
export const AppLayout: React.FC = () => {
  return (
    <div className="flex min-h-screen bg-transparent text-foreground relative overflow-hidden">
      {/* Deep Dark Ambient Background Overlay for Extra Depth */}
      <div className="fixed inset-0 pointer-events-none -z-10 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#1a153a]/40 via-transparent to-transparent opacity-80" />
      
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-4 md:p-8 pt-6 no-scrollbar relative z-0">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
