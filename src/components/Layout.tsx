import React from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Activity, 
  BookOpen, 
  BarChart3, 
  Users, 
  LogOut,
  Moon,
  Sun
} from 'lucide-react';
import { useAuthStore } from '../store/useAuthStore';

const Layout: React.FC = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const menuItems = user?.role === 'STUDENT' ? [
    { name: '总览', icon: LayoutDashboard, path: '/' },
    { name: '智能诊断', icon: Activity, path: '/diagnostic' },
    { name: '个性化训练', icon: BookOpen, path: '/training' },
    { name: '学情分析', icon: BarChart3, path: '/analytics' },
  ] : [
    { name: '班级总览', icon: Users, path: '/teacher' },
    { name: '词表管理', icon: BookOpen, path: '/teacher/vocab' },
  ];

  return (
    <div className="flex h-screen bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-slate-100">
      {/* Sidebar */}
      <aside className="w-64 bg-white dark:bg-slate-800 border-r border-slate-200 dark:border-slate-700">
        <div className="p-6">
          <h1 className="text-xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            EF-Transfer
          </h1>
          <p className="text-xs text-slate-500 mt-1">英法双语认知迁移平台</p>
        </div>
        
        <nav className="mt-4 px-4 space-y-1">
          {menuItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
            >
              <item.icon size={20} />
              <span className="font-medium">{item.name}</span>
            </Link>
          ))}
        </nav>

        <div className="absolute bottom-4 w-64 px-4">
          <button 
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-3 w-full rounded-lg text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
          >
            <LogOut size={20} />
            <span className="font-medium">退出登录</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 flex items-center justify-between px-8">
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-slate-600 dark:text-slate-400">
              当前角色: <span className="text-blue-600">{user?.role}</span>
            </span>
          </div>
          <div className="flex items-center gap-4">
            <button className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-700">
              <Sun size={20} />
            </button>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">
                {user?.username?.[0]}
              </div>
              <span className="text-sm font-medium">{user?.username}</span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default Layout;
