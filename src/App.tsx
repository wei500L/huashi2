import React, { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout';
import { useAuthStore, useUIStore } from './store';
import Dashboard from './pages/dashboard/index';

const Placeholder = ({ name }: { name: string }) => (
  <div className="p-8 border-2 border-dashed border-border rounded-3xl flex flex-col items-center justify-center min-h-[400px] text-muted-foreground bg-muted/5">
    <h2 className="text-xl font-bold text-foreground mb-2">{name} 模块开发中</h2>
    <p>正在连接英法双语语料库与认知加工模型...</p>
  </div>
);

const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const App: React.FC = () => {
  const { login, isAuthenticated } = useAuthStore();
  const { isDarkMode } = useUIStore();
// 初始化明暗模式样式
useEffect(() => {
  const root = window.document.documentElement;
  if (isDarkMode) {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
}, [isDarkMode]);

  useEffect(() => {
    if (!isAuthenticated) {
      login('STUDENT');
    }
  }, [isAuthenticated, login]);

  return (
    <Routes>
      <Route path="/login" element={<div className="h-screen flex items-center justify-center font-bold text-2xl bg-slate-900 text-white">Login Page Placeholder</div>} />
      
      <Route path="/" element={<AuthGuard><AppLayout /></AuthGuard>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="diagnosis" element={<Placeholder name="智能诊断" />} />
        <Route path="training" element={<Placeholder name="个性化训练" />} />
        <Route path="analytics" element={<Placeholder name="学情分析" />} />
        <Route path="teacher" element={<Placeholder name="教师端" />} />
        <Route path="settings" element={<Placeholder name="系统设置" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
