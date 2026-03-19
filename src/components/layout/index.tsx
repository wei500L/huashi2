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
  BookOpen,
  ClipboardList,
  Database
} from 'lucide-react';
import { useAuthStore, useUIStore } from '@/store';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { motion, AnimatePresence, useMotionValue, useSpring } from 'framer-motion';
import { Magnetic } from '@/components/common';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// 1. Sidebar Component
export const Sidebar: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { isSidebarCollapsed, toggleSidebar } = useUIStore();
  const location = useLocation();

  const sections = user?.role === 'STUDENT' ? [
    {
      label: 'Core',
      items: [
        { name: '总览', icon: LayoutDashboard, path: '/dashboard' },
        { name: '智能诊断', icon: Activity, path: '/diagnosis' },
      ]
    },
    {
      label: 'Training',
      items: [
        { name: '个性化训练', icon: GraduationCap, path: '/training' },
        { name: '学情分析', icon: LineChart, path: '/analytics' },
      ]
    },
    {
      label: 'Repository',
      items: [
        { name: '实验任务', icon: ClipboardList, path: '/tasks' },
        { name: '错题库', icon: Database, path: '/errors' },
      ]
    },
    {
      label: 'System',
      items: [
        { name: '系统设置', icon: Settings, path: '/settings' },
      ]
    }
  ] : [
    {
      label: 'Management',
      items: [
        { name: '班级管理', icon: BookOpen, path: '/teacher' },
        { name: '数据监控', icon: LineChart, path: '/monitor' },
      ]
    },
    {
      label: 'System',
      items: [
        { name: '系统设置', icon: Settings, path: '/settings' },
      ]
    }
  ];

  return (
    <aside className={cn(
      "h-[calc(100vh-1.5rem)] my-3 ml-3 flex flex-col transition-all duration-700 z-50 liquid-glass-panel rounded-3xl edge-light fluid-texture",
      isSidebarCollapsed ? "w-20" : "w-64"
    )}>
      <div className="p-8 flex items-center justify-between relative z-10">
        {!isSidebarCollapsed && (
          <motion.div 
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="flex items-center gap-3 group cursor-pointer"
          >
            <div className="relative w-8 h-8 flex items-center justify-center">
              <div className="absolute inset-0 bg-primary/20 rounded-lg rotate-45 group-hover:rotate-90 transition-transform duration-700" />
              <div className="absolute inset-0 border border-primary/50 rounded-lg -rotate-12 group-hover:rotate-0 transition-transform duration-700" />
              <Activity size={16} className="text-primary relative z-10" />
            </div>
            
            <div className="flex flex-col">
              <span className="text-lg font-black tracking-tighter text-slate-900 dark:text-white leading-none">
                EF<span className="text-primary">.</span>
              </span>
              <span className="text-[10px] font-bold tracking-[0.3em] uppercase text-slate-400 dark:text-white/30 leading-none mt-1">
                Transfer
              </span>
            </div>
          </motion.div>
        )}
        <Magnetic strength={0.2}>
          <button onClick={toggleSidebar} className="p-2.5 hover:bg-black/5 dark:hover:bg-white/10 rounded-xl transition-colors backdrop-blur-md border border-slate-200 dark:border-white/5 group">
            <ChevronLeft className={cn("transition-transform duration-500 text-slate-400 dark:text-white/70 group-hover:text-primary dark:group-hover:text-white", isSidebarCollapsed && "rotate-180")} size={20} />
          </button>
        </Magnetic>
      </div>
      
      <nav className="flex-1 px-4 py-4 overflow-y-auto no-scrollbar relative z-10 space-y-8">
        {sections.map((section, idx) => (
          <div key={idx} className="space-y-2">
            {!isSidebarCollapsed && (
              <h4 className="px-4 text-[9px] font-black uppercase tracking-[0.3em] text-slate-400 dark:text-white/20">
                {section.label}
              </h4>
            )}
            <div className="space-y-1">
              {section.items.map((item) => {
                const isActive = location.pathname === item.path;
                return (
                  <motion.div key={item.path} whileHover={{ x: 4 }} whileTap={{ scale: 0.98 }}>
                    <Link to={item.path} className={cn(
                      "flex items-center gap-3 px-4 py-3 rounded-2xl transition-all duration-300 group relative",
                      isActive ? "text-primary font-black" : "text-slate-500 dark:text-white/50 hover:text-primary dark:hover:text-white"
                    )}>
                      {isActive && (
                        <motion.div 
                          layoutId="active-pill"
                          className="absolute inset-0 bg-primary/[0.08] border border-primary/20 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.1)] dark:bg-primary/10 dark:border-primary/30"
                          transition={{ type: "spring", stiffness: 350, damping: 25 }}
                        />
                      )}
                      <item.icon size={18} className={cn(
                        "transition-all duration-300 group-hover:scale-110 relative z-10",
                        isActive ? "drop-shadow-[0_0_8px_rgba(139,92,246,0.8)]" : "opacity-70 group-hover:opacity-100"
                      )} />
                      {!isSidebarCollapsed && <span className="relative z-10 tracking-wide text-xs">{item.name}</span>}
                    </Link>
                  </motion.div>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-4 relative z-10 mt-auto border-t border-white/5">
        <Magnetic strength={0.1}>
          <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-rose-500 dark:text-rose-400 hover:bg-rose-500/10 hover:text-rose-600 dark:hover:text-rose-300 transition-all border border-transparent hover:border-rose-500/20 group font-bold">
            <LogOut size={18} className="group-hover:-translate-x-1 transition-transform" />
            {!isSidebarCollapsed && <span className="uppercase tracking-widest text-[9px]">Eject Session</span>}
          </button>
        </Magnetic>
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
            placeholder="Search neural patterns..." 
            className="w-full bg-white/50 dark:bg-black/30 border border-slate-200 dark:border-white/5 focus:border-primary/40 focus:bg-white dark:focus:bg-black/50 rounded-full py-2.5 pl-11 pr-4 text-sm transition-all duration-500 focus:outline-none focus:ring-4 focus:ring-primary/5 text-slate-900 dark:text-white placeholder:text-slate-400 dark:placeholder:text-white/20" 
          />
        </div>
      </div>
      <div className="flex items-center gap-5 relative z-10">
        <Magnetic strength={0.2}>
          <button className="p-2.5 hover:bg-black/5 dark:hover:bg-white/5 rounded-full transition-all relative group border border-transparent hover:border-slate-200 dark:hover:border-white/10">
            <Bell size={20} className="text-slate-500 dark:text-white/50 group-hover:text-primary dark:group-hover:text-white transition-colors" />
            <div className="absolute top-2.5 right-2.5 w-1.5 h-1.5 bg-rose-500 rounded-full animate-pulse shadow-[0_0_10px_rgba(244,63,94,1)]" />
          </button>
        </Magnetic>
        <Magnetic strength={0.2}>
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
        </Magnetic>
        <div className="w-px h-8 bg-slate-200 dark:bg-white/5 mx-1" />
        <Magnetic strength={0.1}>
          <div className="flex items-center gap-3 pl-1 cursor-pointer group">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-black text-slate-900 dark:text-white/90 group-hover:text-primary transition-colors">{user?.username || 'Guest'}</p>
              <p className="text-[9px] text-slate-400 dark:text-white/30 uppercase tracking-[0.3em] font-bold mt-0.5">{user?.role || 'USER'}</p>
            </div>
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-primary via-purple-600 to-accent flex items-center justify-center text-white font-black shadow-[0_0_20px_rgba(139,92,246,0.4)] border border-white/20 group-hover:scale-105 transition-transform duration-300 relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-tr from-white/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
              {user?.username?.[0] || 'G'}
            </div>
          </div>
        </Magnetic>
      </div>
    </header>
  );
};

// 3. Layout Container
export const AppLayout: React.FC = () => {
  const cursorX = useMotionValue(-100);
  const cursorY = useMotionValue(-100);
  const [cursorType, setCursorType] = React.useState<'default' | 'pointer' | 'chart'>('default');
  const [isClicked, setIsClicked] = React.useState(false);
  
  const ambientSpringConfig = { damping: 40, stiffness: 150, mass: 0.5 };
  const ambientXSpring = useSpring(cursorX, ambientSpringConfig);
  const ambientYSpring = useSpring(cursorY, ambientSpringConfig);

  const cursorSpringConfig = { damping: 25, stiffness: 400, mass: 0.2 };
  const geometricXSpring = useSpring(cursorX, cursorSpringConfig);
  const geometricYSpring = useSpring(cursorY, cursorSpringConfig);

  React.useEffect(() => {
    const moveCursor = (e: MouseEvent) => {
      cursorX.set(e.clientX);
      cursorY.set(e.clientY);
      
      const target = e.target as HTMLElement;
      if (target.closest('button, a, [role="button"]')) {
        setCursorType('pointer');
      } else if (target.closest('.echarts-for-react')) {
        setCursorType('chart');
      } else {
        setCursorType('default');
      }
    };
    const handleMouseDown = () => setIsClicked(true);
    const handleMouseUp = () => setIsClicked(false);

    window.addEventListener('mousemove', moveCursor);
    window.addEventListener('mousedown', handleMouseDown);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', moveCursor);
      window.removeEventListener('mousedown', handleMouseDown);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [cursorX, cursorY]);

  return (
    <div className="flex h-screen bg-transparent text-foreground relative overflow-hidden transition-colors duration-500 cursor-none">
      <div className="fixed inset-0 z-[99999] pointer-events-none bg-primary animate-page-reveal" />

      {/* Advanced Custom Cursor with Elegant States */}
      <motion.div 
        className="fixed top-0 left-0 border-2 border-primary rounded-full pointer-events-none z-[9999] mix-blend-difference hidden md:block"
        animate={{ 
          width: cursorType === 'chart' ? 42 : (cursorType === 'pointer' ? 60 : 32), 
          height: cursorType === 'chart' ? 42 : (cursorType === 'pointer' ? 60 : 32),
          borderRadius: cursorType === 'chart' ? "35%" : "50%",
          rotate: cursorType === 'chart' ? 45 : 0,
          opacity: isClicked ? 0.5 : 1
        }}
        transition={{ type: "spring", stiffness: 350, damping: 25 }}
        style={{ 
          x: geometricXSpring, 
          y: geometricYSpring, 
          translateX: "-50%", 
          translateY: "-50%" 
        }}
      />
      <motion.div 
        className="fixed top-0 left-0 w-1.5 h-1.5 bg-primary rounded-full pointer-events-none z-[10000] hidden md:block"
        animate={{ scale: isClicked ? 2 : 1 }}
        style={{ 
          x: cursorX, 
          y: cursorY, 
          translateX: "-50%", 
          translateY: "-50%" 
        }}
      />

      <div className="fixed inset-0 -z-20 bg-[#f8fafc] dark:bg-[#030208] transition-colors duration-700" />
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        <motion.div 
          className="absolute top-0 left-0 w-[800px] h-[800px] bg-primary/10 dark:bg-primary/20 rounded-full blur-[120px] opacity-50 transition-colors duration-700"
          style={{ x: ambientXSpring, y: ambientYSpring, translateX: "-50%", translateY: "-50%" }}
        />
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-400/10 dark:bg-primary/10 rounded-full blur-[100px] animate-float opacity-40 transition-colors duration-700" />
        <div className="absolute bottom-[5%] right-[-5%] w-[35%] h-[35%] bg-purple-400/10 dark:bg-accent/10 rounded-full blur-[100px] animate-float-delayed opacity-30 transition-colors duration-700" />
        <div className="absolute top-[20%] right-[15%] w-[25%] h-[25%] bg-indigo-400/10 dark:bg-purple-600/10 rounded-full blur-[100px] animate-pulse-slow opacity-30 transition-colors duration-700" />
      </div>

      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-6 md:p-10 no-scrollbar relative z-0">
          <motion.div 
            initial={{ opacity: 0, y: 30, filter: "blur(10px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{ duration: 1.2, ease: [0.16, 1, 0.3, 1], delay: 0.2 }}
            className="max-w-7xl mx-auto"
          >
            <Outlet />
          </motion.div>
        </main>
      </div>
    </div>
  );
};
