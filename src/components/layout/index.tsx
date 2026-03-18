import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
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
    { name: '实验监控', icon: Activity, path: '/teacher/monitoring' },
  ];

  return (
    <aside className={cn(
      "h-screen bg-card border-r border-border flex flex-col transition-all duration-300 z-50",
      isSidebarCollapsed ? "w-20" : "w-64"
    )}>
      <div className="p-6 flex items-center justify-between">
        {!isSidebarCollapsed && (
          <h1 className="text-xl font-black bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
            EF-Transfer
          </h1>
        )}
        <button 
          onClick={toggleSidebar}
          className="p-2 hover:bg-muted rounded-lg transition-colors"
        >
          <ChevronLeft className={cn("transition-transform", isSidebarCollapsed && "rotate-180")} size={20} />
        </button>
      </div>

      <nav className="flex-1 px-4 space-y-1 overflow-y-auto no-scrollbar">
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={cn(
                "flex items-center gap-3 px-4 py-3 rounded-xl transition-all group",
                isActive 
                  ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" 
                  : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <item.icon size={20} className={cn("shrink-0", isActive ? "" : "group-hover:scale-110 transition-transform")} />
              {!isSidebarCollapsed && <span className="font-medium truncate">{item.name}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-border space-y-2">
        <Link
          to="/settings"
          className="flex items-center gap-3 px-4 py-3 rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground transition-all"
        >
          <Settings size={20} className="shrink-0" />
          {!isSidebarCollapsed && <span className="font-medium">系统设置</span>}
        </Link>
        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/10 transition-all"
        >
          <LogOut size={20} className="shrink-0" />
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
    <header className="h-16 bg-card/80 backdrop-blur-md border-b border-border flex items-center justify-between px-8 sticky top-0 z-40">
      <div className="flex items-center gap-4 flex-1">
        <div className="relative max-w-md w-full hidden md:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16} />
          <input 
            type="text" 
            placeholder="搜索词汇、例句或诊断结果..." 
            className="w-full bg-muted/50 border-none rounded-full py-2 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/20 transition-all"
          />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button 
          onClick={toggleDarkMode}
          className="p-2 hover:bg-muted rounded-full transition-colors text-muted-foreground"
        >
          {isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
        </button>
        <button className="p-2 hover:bg-muted rounded-full transition-colors text-muted-foreground relative">
          <Bell size={20} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-card" />
        </button>
        
        <div className="h-8 w-px bg-border mx-2" />

        <div className="flex items-center gap-3 pl-2">
          <div className="text-right hidden sm:block">
            <p className="text-sm font-bold leading-tight">{user?.username || 'Guest'}</p>
            <p className="text-[10px] text-muted-foreground font-medium tracking-wide uppercase">{user?.role || 'Visitor'}</p>
          </div>
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-white font-bold shadow-md">
            {user?.username?.[0] || 'U'}
          </div>
        </div>
      </div>
    </header>
  );
};
