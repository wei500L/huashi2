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
import { motion, AnimatePresence } from 'framer-motion';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 1. Sidebar Component with Liquid Indicator
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
      "h-[calc(100vh-1.5rem)] my-3 ml-3 flex flex-col transition-all duration-500 z-50 liquid-glass-panel rounded-3xl edge-light fluid-texture",
      isSidebarCollapsed ? "w-20" : "w-64"
    )}>
      <div className="p-6 flex items-center justify-between relative z-10">
        {!isSidebarCollapsed && (
          <motion.h1 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-xl font-black bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent dark:text-transparent drop-shadow-sm dark:drop-shadow-[0_0_15px_rgba(139,92,246,0.5)]"
          >
            EF-Transfer
          </motion.h1>
        )}
        <button onClick={toggleSidebar} className="p-2 hover:bg-black/5 dark:hover:bg-white/10 rounded-xl transition-colors backdrop-blur-md border border-slate-200 dark:border-white/5 group">
          <ChevronLeft className={cn("transition-transform duration-500 text-slate-400 dark:text-white/70 group-hover:text-primary dark:group-hover:text-white", isSidebarCollapsed && "rotate-180")} size={20} />
        </button>
      </div>
      
      <nav className="flex-1 px-4 space-y-2 overflow-y-auto no-scrollbar relative z-10 mt-4">
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <Link key={item.path} to={item.path} className={cn(
              "flex items-center gap-3 px-4 py-3 rounded-2xl transition-all duration-300 group relative",
              isActive ? "text-primary font-bold" : "text-slate-500 dark:text-white/50 hover:text-primary dark:hover:text-white"
            )}>
              {isActive && (
                <motion.div 
                  layoutId="active-pill"
                  className="absolute inset-0 bg-primary/10 border border-primary/30 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.2)]"
                  transition={{ type: "spring", stiffness: 300, damping: 30 }}
                />
              )}
              {isActive && (
                <motion.div 
                  layoutId="active-dot"
                  className="absolute left-0 w-1 h-6 bg-primary rounded-r-full shadow-[0_0_15px_rgba(139,92,246,1)]"
                  transition={{ type: "spring", stiffness: 300, damping: 30 }}
                />
              )}
              
              <item.icon size={20} className={cn(
                "transition-all duration-300 group-hover:scale-110 relative z-10",
                isActive ? "drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]" : "opacity-70 group-hover:opacity-100"
              )} />
              {!isSidebarCollapsed && <span className="relative z-10">{item.name}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="p-4 relative z-10 mt-auto">
        <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-rose-500 dark:text-rose-400 hover:bg-rose-500/10 hover:text-rose-600 dark:hover:text-rose-300 transition-all border border-transparent hover:border-rose-500/20 group">
          <LogOut size={20} className="group-hover:-translate-x-1 transition-transform" />
          {!isSidebarCollapsed && <span className="font-bold uppercase tracking-wider text-xs">Shutdown System</span>}
        </button>
      </div>
    </aside>
  );
};

// 2. Topbar Component
export const Topbar: React.FC = () => {
  const { user } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();

  return (
    <header className="h-16 mt-3 mx-4 lg:mx-8 flex items-center justify-between px-6 sticky top-3 z-40 liquid-glass rounded-2xl edge-light fluid-texture">
      <div className="flex items-center gap-4 flex-1 relative z-10">
        <div className="relative max-w-md w-full hidden md:block group">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30 group-focus-within:text-primary transition-colors duration-300" size={16} />
          <input 
            type="text" 
            placeholder="Explore global analytics..." 
            className="w-full bg-white/50 dark:bg-black/30 border border-slate-200 dark:border-white/5 focus:border-primary/40 focus:bg-white dark:focus:bg-black/50 rounded-full py-2.5 pl-11 pr-4 text-sm transition-all duration-500 focus:outline-none focus:ring-4 focus:ring-primary/5 text-slate-900 dark:text-white placeholder:text-slate-400 dark:placeholder:text-white/20" 
          />
        </div>
      </div>
      <div className="flex items-center gap-5 relative z-10">
        <button className="p-2.5 hover:bg-black/5 dark:hover:bg-white/5 rounded-full transition-all relative group border border-transparent hover:border-slate-200 dark:hover:border-white/10">
          <Bell size={20} className="text-slate-500 dark:text-white/50 group-hover:text-primary dark:group-hover:text-white transition-colors" />
          <div className="absolute top-2 right-2 w-2 h-2 bg-rose-500 rounded-full animate-pulse shadow-[0_0_10px_rgba(244,63,94,1)]" />
        </button>
        <button onClick={toggleDarkMode} className="p-2.5 hover:bg-black/5 dark:hover:bg-white/5 rounded-full transition-all group border border-transparent hover:border-slate-200 dark:hover:border-white/10 overflow-hidden">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={isDarkMode ? 'dark' : 'light'}
              initial={{ y: 20, opacity: 0, rotate: 45 }}
              animate={{ y: 0, opacity: 1, rotate: 0 }}
              exit={{ y: -20, opacity: 0, rotate: -45 }}
              transition={{ duration: 0.3, ease: "circOut" }}
            >
              {isDarkMode ? (
                <Sun size={20} className="text-white/50 group-hover:text-amber-400 transition-colors" />
              ) : (
                <Moon size={20} className="text-slate-400 group-hover:text-blue-500 transition-colors" />
              )}
            </motion.div>
          </AnimatePresence>
        </button>
        <div className="w-px h-8 bg-slate-200 dark:bg-white/5 mx-1" />
        <div className="flex items-center gap-3 pl-1 cursor-pointer group">
          <div className="text-right hidden sm:block">
            <p className="text-sm font-black text-slate-900 dark:text-white/90 group-hover:text-primary transition-colors">{user?.username || 'Guest'}</p>
            <p className="text-[9px] text-slate-400 dark:text-white/30 uppercase tracking-[0.2em] font-bold mt-0.5">{user?.role || 'USER'}</p>
          </div>
          <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-primary via-purple-600 to-accent flex items-center justify-center text-white font-black shadow-[0_0_20px_rgba(139,92,246,0.4)] border border-white/20 group-hover:scale-105 transition-transform duration-300">
            {user?.username?.[0] || 'G'}
          </div>
        </div>
      </div>
    </header>
  );
};

// 3. Layout Container with Dynamic Environmental Blobs
import { useMotionValue, useSpring } from 'framer-motion';

export const AppLayout: React.FC = () => {
  const cursorX = useMotionValue(-100);
  const cursorY = useMotionValue(-100);
  
  const springConfig = { damping: 40, stiffness: 150, mass: 0.5 };
  const cursorXSpring = useSpring(cursorX, springConfig);
  const cursorYSpring = useSpring(cursorY, springConfig);

  React.useEffect(() => {
    const moveCursor = (e: MouseEvent) => {
      cursorX.set(e.clientX);
      cursorY.set(e.clientY);
    };
    window.addEventListener('mousemove', moveCursor);
    return () => window.removeEventListener('mousemove', moveCursor);
  }, [cursorX, cursorY]);

  return (
    <div className="flex min-h-screen bg-transparent text-foreground relative overflow-hidden transition-colors duration-500">
      {/* Background Deep Layers - Responsive */}
      <div className="fixed inset-0 -z-20 bg-[#f8fafc] dark:bg-[#030208] transition-colors duration-700" />
      
      {/* Dynamic Environmental Blobs */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        {/* Interactive Mouse Blob */}
        <motion.div 
          className="absolute top-0 left-0 w-[800px] h-[800px] bg-primary/10 dark:bg-primary/20 rounded-full blur-[160px] opacity-50 transition-colors duration-700"
          style={{ 
            x: cursorXSpring, 
            y: cursorYSpring, 
            translateX: "-50%", 
            translateY: "-50%" 
          }}
        />
        {/* Static Animated Blobs */}
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-400/10 dark:bg-primary/10 rounded-full blur-[120px] animate-float opacity-40 transition-colors duration-700" />
        <div className="absolute bottom-[5%] right-[-5%] w-[35%] h-[35%] bg-purple-400/10 dark:bg-accent/10 rounded-full blur-[100px] animate-float-delayed opacity-30 transition-colors duration-700" />
        <div className="absolute top-[20%] right-[15%] w-[25%] h-[25%] bg-indigo-400/10 dark:bg-purple-600/10 rounded-full blur-[100px] animate-pulse-slow opacity-30 transition-colors duration-700" />
      </div>

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
