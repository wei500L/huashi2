import React, { Suspense } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppLayout } from './components/layout';
import { useAuthStore, useUIStore } from './store';
import { SESSION_CHANGE_EVENT } from './lib/session';
import { homePathForCapabilities, userHasCapability } from './lib/format';
import type { Capability } from './lib/contracts';

const Login = React.lazy(() => import('./pages/Login'));
const Dashboard = React.lazy(() => import('./pages/dashboard/index'));
const DiagnosisPage = React.lazy(() => import('./pages/diagnosis/index'));
const TrainingPage = React.lazy(() => import('./pages/training/index'));
const AnalyticsPage = React.lazy(() => import('./pages/analytics/index'));
const ErrorsPage = React.lazy(() => import('./pages/student/Errors'));
const SettingsPage = React.lazy(() => import('./pages/student/Settings'));
const TeacherClassesPage = React.lazy(() => import('./pages/teacher/Classes'));
const TeacherClassDetailPage = React.lazy(() => import('./pages/teacher/ClassDetail'));
const TeacherStudentDetailPage = React.lazy(() => import('./pages/teacher/StudentDetail'));
const TeacherTemplatesPage = React.lazy(() => import('./pages/teacher/Templates'));
const TeacherLexicalPairsPage = React.lazy(() => import('./pages/teacher/LexicalPairs'));
const TeacherLexicalListsPage = React.lazy(() => import('./pages/teacher/LexicalLists'));
const TeacherInterventionsPage = React.lazy(() => import('./pages/teacher/Interventions'));
const AdminUsersPage = React.lazy(() => import('./pages/admin/index'));
const AdminConfigCenterPage = React.lazy(() => import('./pages/admin/ConfigCenter'));

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

const RequireCapability: React.FC<{ capability: Capability; children: React.ReactNode }> = ({ capability, children }) => {
  const user = useAuthStore((state) => state.user);
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return userHasCapability(user, capability) ? <>{children}</> : <Navigate to={homePathForCapabilities(user.capabilities)} replace />;
};

const HomeRedirect: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  return <Navigate to={homePathForCapabilities(user?.capabilities)} replace />;
};

const withSuspense = (node: React.ReactNode) => <Suspense fallback={<BootScreen />}>{node}</Suspense>;

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
      <Route
        path="/login"
        element={status === 'authenticated' && user ? <Navigate to={homePathForCapabilities(user.capabilities)} replace /> : withSuspense(<Login />)}
      />

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
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<Dashboard />)}</RequireCapability>
          }
        />
        <Route
          path="diagnosis"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<DiagnosisPage />)}</RequireCapability>
          }
        />
        <Route
          path="training"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<TrainingPage />)}</RequireCapability>
          }
        />
        <Route
          path="analytics"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<AnalyticsPage />)}</RequireCapability>
          }
        />
        <Route
          path="errors"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<ErrorsPage />)}</RequireCapability>
          }
        />

        <Route
          path="teacher/classes"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassesPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes/:classId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassDetailPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes/:classId/students/:studentUserId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherStudentDetailPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/diagnosis-templates"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherTemplatesPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-pairs"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalPairsPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-lists"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalListsPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/interventions"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherInterventionsPage />)}</RequireCapability>
          }
        />

        <Route
          path="admin/config-center"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminConfigCenterPage />)}</RequireCapability>
          }
        />

        <Route
          path="admin/users"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminUsersPage />)}</RequireCapability>
          }
        />

        <Route path="teacher" element={<Navigate to="/teacher/classes" replace />} />
        <Route path="monitor" element={<Navigate to="/teacher/interventions" replace />} />
        <Route path="settings" element={withSuspense(<SettingsPage />)} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
