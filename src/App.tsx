import React, { useEffect } from 'react';
import { Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import { Sidebar, Topbar } from './components/layout';
import { useAuthStore } from './store';

// 1. 布局外壳组件
export const AppLayout: React.FC = () => {
  const { isSidebarCollapsed } = useAuthStore(); // 注意：此处应使用 uiStore，为简化演示暂用 auth
  
  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-4 md:p-8 no-scrollbar">
          <div className="max-w-7xl mx-auto animate-in fade-in slide-in-from-bottom-2 duration-500">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

// 2. 路由守护组件
const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

// 3. 基础占位组件
const Placeholder = ({ name }: { name: string }) => (
  <div className="p-8 border-2 border-dashed border-border rounded-3xl flex flex-col items-center justify-center min-h-[400px] text-muted-foreground bg-muted/5">
    <h2 className="text-xl font-bold text-foreground mb-2">{name} 模块开发中</h2>
    <p>正在连接英法双语语料库与认知加工模型...</p>
  </div>
);

// 4. 主 App 组件
const App: React.FC = () => {
  const { login, isAuthenticated } = useAuthStore();

  // MVP 测试：自动登录
  useEffect(() => {
    if (!isAuthenticated) {
      login({ id: '1', username: 'ResearchUser', role: 'STUDENT', token: 'mock-jwt' });
    }
  }, [isAuthenticated, login]);

  return (
    <Routes>
      <Route path="/login" element={<div className="h-screen flex items-center justify-center font-bold text-2xl">Login Page Placeholder</div>} />
      
      <Route path="/" element={<AuthGuard><AppLayout /></AuthGuard>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Placeholder name="总览" />} />
        <Route path="diagnosis" element={<Placeholder name="智能诊断" />} />
        <Route path="training" element={<Placeholder name="个性化训练" />} />
        <Route path="analytics" element={<Placeholder name="学情分析" />} />
        <Route path="settings" element={<Placeholder name="系统设置" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
