import React from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppLayout } from './components/layout';
import { useAuthStore, useUIStore } from './store';
import { SESSION_CHANGE_EVENT } from './lib/session';
import { roleHomePath } from './lib/format';
import Login from './pages/Login';
import Dashboard from './pages/dashboard/index';
import DiagnosisPage from './pages/diagnosis/index';
import TrainingPage from './pages/training/index';
import AnalyticsPage from './pages/analytics/index';
import ErrorsPage from './pages/student/Errors';
import SettingsPage from './pages/student/Settings';
import TeacherClassesPage from './pages/teacher/Classes';
import TeacherClassDetailPage from './pages/teacher/ClassDetail';
import TeacherStudentDetailPage from './pages/teacher/StudentDetail';
import TeacherTemplatesPage from './pages/teacher/Templates';
import TeacherLexicalPairsPage from './pages/teacher/LexicalPairs';
import TeacherLexicalListsPage from './pages/teacher/LexicalLists';
import TeacherInterventionsPage from './pages/teacher/Interventions';
import AdminUsersPage from './pages/admin/index';
import AdminConfigCenterPage from './pages/admin/ConfigCenter';
import type { Role } from './lib/contracts';

const BootScreen: React.FC = () => (
  <div className="min-h-screen flex items-center justify-center bg-background">
    <div className="text-[10px] uppercase tracking-[0.4em] text-slate-400 dark:text-white/30">initializing session</div>
  </div>
);

const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  const authenticated = useAuthStore((state) => state.status === 'authenticated' && !!state.session?.accessToken);
  return authenticated ? <>{children}</> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
};

const RequireRole: React.FC<{ roles: Role[]; children: React.ReactNode }> = ({ roles, children }) => {
  const user = useAuthStore((state) => state.user);
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return roles.includes(user.primaryRole) ? <>{children}</> : <Navigate to={roleHomePath(user.primaryRole)} replace />;
};

const HomeRedirect: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  return <Navigate to={roleHomePath(user?.primaryRole)} replace />;
};

const App: React.FC = () => {
  const initialize = useAuthStore((state) => state.initialize);
  const syncFromStorage = useAuthStore((state) => state.syncFromStorage);
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const { isDarkMode } = useUIStore();

  React.useEffect(() => {
    void initialize();
  }, [initialize]);

  React.useEffect(() => {
    const handler = () => syncFromStorage();
    window.addEventListener(SESSION_CHANGE_EVENT, handler);
    return () => window.removeEventListener(SESSION_CHANGE_EVENT, handler);
  }, [syncFromStorage]);

  React.useEffect(() => {
    const root = window.document.documentElement;
    if (isDarkMode) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [isDarkMode]);

  if (status === 'idle' || status === 'loading') {
    return <BootScreen />;
  }

  return (
    <Routes>
      <Route path="/login" element={status === 'authenticated' && user ? <Navigate to={roleHomePath(user.primaryRole)} replace /> : <Login />} />

      <Route
        path="/"
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<HomeRedirect />} />

        <Route
          path="dashboard"
          element={
            <RequireRole roles={['STUDENT']}>
              <Dashboard />
            </RequireRole>
          }
        />
        <Route
          path="diagnosis"
          element={
            <RequireRole roles={['STUDENT']}>
              <DiagnosisPage />
            </RequireRole>
          }
        />
        <Route
          path="training"
          element={
            <RequireRole roles={['STUDENT']}>
              <TrainingPage />
            </RequireRole>
          }
        />
        <Route
          path="analytics"
          element={
            <RequireRole roles={['STUDENT']}>
              <AnalyticsPage />
            </RequireRole>
          }
        />
        <Route
          path="errors"
          element={
            <RequireRole roles={['STUDENT']}>
              <ErrorsPage />
            </RequireRole>
          }
        />

        <Route
          path="teacher/classes"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherClassesPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/classes/:classId"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherClassDetailPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/classes/:classId/students/:studentUserId"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherStudentDetailPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/diagnosis-templates"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherTemplatesPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/lexical-pairs"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherLexicalPairsPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/lexical-lists"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherLexicalListsPage />
            </RequireRole>
          }
        />
        <Route
          path="teacher/interventions"
          element={
            <RequireRole roles={['TEACHER']}>
              <TeacherInterventionsPage />
            </RequireRole>
          }
        />

        <Route
          path="admin/config-center"
          element={
            <RequireRole roles={['ADMIN']}>
              <AdminConfigCenterPage />
            </RequireRole>
          }
        />

        <Route
          path="admin/users"
          element={
            <RequireRole roles={['ADMIN']}>
              <AdminUsersPage />
            </RequireRole>
          }
        />

        <Route path="teacher" element={<Navigate to="/teacher/classes" replace />} />
        <Route path="monitor" element={<Navigate to="/teacher/interventions" replace />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
