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
      "h-screen bg-card border-r border-border flex flex-col transition-all duration-300 z-50",
      isSidebarCollapsed ? "w-20" : "w-64"
    )}>
      <div className="p-6 flex items-center justify-between">
        {!isSidebarCollapsed && (
          <h1 className="text-xl font-black bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
            EF-Transfer
          </h1>
        )}
        <button onClick={toggleSidebar} className="p-2 hover:bg-muted rounded-lg transition-colors">
          <ChevronLeft className={cn("transition-transform", isSidebarCollapsed && "rotate-180")} size={20} />
        </button>
      </div>
      <nav className="flex-1 px-4 space-y-1 overflow-y-auto no-scrollbar">
        {menuItems.map((item) => (
          <Link key={item.path} to={item.path} className={cn(
            "flex items-center gap-3 px-4 py-3 rounded-xl transition-all group",
            location.pathname === item.path ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
          )}>
            <item.icon size={20} />
            {!isSidebarCollapsed && <span className="font-medium">{item.name}</span>}
          </Link>
        ))}
      </nav>
      <div className="p-4 border-t border-border">
        <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-rose-500 hover:bg-rose-50 transition-all">
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
    <header className="h-16 bg-card/80 backdrop-blur-md border-b border-border flex items-center justify-between px-8 sticky top-0 z-40">
      <div className="flex items-center gap-4 flex-1">
        <div className="relative max-w-md w-full hidden md:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16} />
          <input type="text" placeholder="搜索..." className="w-full bg-muted/50 border-none rounded-full py-2 pl-10 pr-4 text-sm" />
        </div>
      </div>
      <div className="flex items-center gap-3">
        <button onClick={toggleDarkMode} className="p-2 hover:bg-muted rounded-full">
          {isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
        </button>
        <div className="flex items-center gap-3 pl-2">
          <div className="text-right hidden sm:block">
            <p className="text-sm font-bold leading-tight">{user?.username}</p>
            <p className="text-[10px] text-muted-foreground uppercase">{user?.role}</p>
          </div>
          <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold">
            {user?.username?.[0]}
          </div>
        </div>
      </div>
    </header>
  );
};

// 3. 布局容器组件
export const AppLayout: React.FC = () => {
  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-4 md:p-8 no-scrollbar">
          <div className="max-w-7xl mx-auto animate-in fade-in duration-500">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
