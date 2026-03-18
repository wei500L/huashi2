import React, { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout'; // 修改为从 layout 文件夹导入
import { useAuthStore, useUIStore } from './store'; // 引入 uiStore

// 基础占位组件
const Placeholder = ({ name }: { name: string }) => (
  <div className="p-8 border-2 border-dashed border-border rounded-3xl flex flex-col items-center justify-center min-h-[400px] text-muted-foreground bg-muted/5">
    <h2 className="text-xl font-bold text-foreground mb-2">{name} 模块开发中</h2>
    <p>正在连接英法双语语料库与认知加工模型...</p>
  </div>
);

// 延迟加载组件（演示用途）
import Dashboard from './pages/dashboard';
import Diagnosis from './pages/diagnosis';
import Training from './pages/training';
import Analytics from './pages/analytics';
import AdminPanel from './pages/admin';

// 路由守护组件
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
    if (isDarkMode) root.classList.add('dark');
    else root.classList.remove('dark');
  }, [isDarkMode]);

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
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="diagnosis" element={<Diagnosis />} />
        <Route path="training" element={<Training />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="teacher" element={<AdminPanel />} />
        <Route path="settings" element={<Placeholder name="系统设置" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
