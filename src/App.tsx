import React, { Suspense } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { AppLayout } from './components/layout';
import { RouteSkeleton } from './components/common';
import i18n from './lib/i18n';
import { buildDocumentTitle } from './lib/page-title';
import { AUTH_EXPIRED_EVENT, SESSION_CHANGE_EVENT, hasPendingAuthExpired } from './lib/session';
import { useAuthStore, useUIStore } from './store';
import { userHasCapability } from './lib/format';
import type { Capability, CurrentUserVO } from './lib/contracts';
import {
  getPreferredWorkspaceForUser,
  homePathForWorkspace,
  resolveActiveWorkspace,
} from './lib/workspaces';
import type { WorkspaceId } from './lib/workspaces';

const Login = React.lazy(() => import('./pages/Login'));
const Register = React.lazy(() => import('./pages/Register'));
const AccountActionPage = React.lazy(() => import('./pages/AccountAction'));
const Dashboard = React.lazy(() => import('./pages/dashboard/index'));
const DiagnosisPage = React.lazy(() => import('./pages/diagnosis/index'));
const TrainingPage = React.lazy(() => import('./pages/training/index'));
const AnalyticsPage = React.lazy(() => import('./pages/analytics/index'));
const ErrorsPage = React.lazy(() => import('./pages/student/Errors'));
const HistoryPage = React.lazy(() => import('./pages/student/History'));
const StudentAssessmentsPage = React.lazy(() => import('./pages/student/Assessments'));
const StudentAssessmentAttemptPage = React.lazy(() => import('./pages/student/AssessmentAttempt'));
const StudentAssessmentResultPage = React.lazy(() => import('./pages/student/AssessmentResult'));
const SettingsPage = React.lazy(() => import('./pages/student/Settings'));
const TeacherWorkspacePage = React.lazy(() => import('./pages/teacher/Workspace'));
const TeacherClassesPage = React.lazy(() => import('./pages/teacher/Classes'));
const TeacherClassEditorPage = React.lazy(() => import('./pages/teacher/ClassEditor'));
const TeacherAssessmentsPage = React.lazy(() => import('./pages/teacher/Assessments'));
const TeacherAssessmentEditorPage = React.lazy(() => import('./pages/teacher/AssessmentEditor'));
const TeacherAssessmentPublishDetailPage = React.lazy(() => import('./pages/teacher/AssessmentPublishDetail'));
const TeacherAssessmentAttemptResultPage = React.lazy(() => import('./pages/teacher/AssessmentAttemptResult'));
const TeacherClassDetailPage = React.lazy(() => import('./pages/teacher/ClassDetail'));
const TeacherStudentDetailPage = React.lazy(() => import('./pages/teacher/StudentDetail'));
const TeacherTemplatesPage = React.lazy(() => import('./pages/teacher/Templates'));
const TeacherTemplateDraftEditorPage = React.lazy(() => import('./pages/teacher/TemplateDraftEditor'));
const TeacherLexicalPairsPage = React.lazy(() => import('./pages/teacher/LexicalPairs'));
const TeacherLexicalPairEditorPage = React.lazy(() => import('./pages/teacher/LexicalPairEditor'));
const TeacherLexicalPairImportsPage = React.lazy(() => import('./pages/teacher/LexicalPairImports'));
const TeacherLexicalListsPage = React.lazy(() => import('./pages/teacher/LexicalLists'));
const TeacherInterventionsPage = React.lazy(() => import('./pages/teacher/Interventions'));
const AdminUsersPage = React.lazy(() => import('./pages/admin/index'));
const AdminConfigCenterPage = React.lazy(() => import('./pages/admin/ConfigCenter'));
const AdminLexicalPairsPage = React.lazy(() => import('./pages/admin/LexicalPairs'));
const AdminLexicalPairEditorPage = React.lazy(() => import('./pages/admin/LexicalPairEditor'));
const AdminLexicalPairImportsPage = React.lazy(() => import('./pages/admin/LexicalPairImports'));

const BootScreen: React.FC = () => <RouteSkeleton />;

function resolveHomePath(
  user: Pick<CurrentUserVO, 'id' | 'username' | 'primaryRole' | 'capabilities'> | null | undefined,
  pathname: string,
  activeWorkspace: WorkspaceId | null,
  preferredWorkspaceByUser: Record<string, WorkspaceId>
): string {
  const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
  const currentWorkspace = resolveActiveWorkspace({
    user,
    pathname,
    activeWorkspace,
    preferredWorkspace,
  });
  return homePathForWorkspace(currentWorkspace);
}

const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  const authenticated = useAuthStore((state) => state.status === 'authenticated' && !!state.session?.accessToken);
  return authenticated ? (
    <>{children}</>
  ) : (
    <Navigate
      to="/login"
      replace
      state={hasPendingAuthExpired() ? { from: location.pathname, expired: true } : { from: location.pathname }}
    />
  );
};

const RequireCapability: React.FC<{ capability: Capability; children: React.ReactNode }> = ({ capability, children }) => {
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const activeWorkspace = useUIStore((state) => state.activeWorkspace);
  const preferredWorkspaceByUser = useUIStore((state) => state.preferredWorkspaceByUser);
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return userHasCapability(user, capability) ? (
    <>{children}</>
  ) : (
    <Navigate
      to={resolveHomePath(user, location.pathname, activeWorkspace, preferredWorkspaceByUser)}
      replace
    />
  );
};

const HomeRedirect: React.FC = () => {
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const activeWorkspace = useUIStore((state) => state.activeWorkspace);
  const preferredWorkspaceByUser = useUIStore((state) => state.preferredWorkspaceByUser);
  return <Navigate to={resolveHomePath(user, location.pathname, activeWorkspace, preferredWorkspaceByUser)} replace />;
};

const withSuspense = (node: React.ReactNode) => <Suspense fallback={<BootScreen />}>{node}</Suspense>;

const App: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const initialize = useAuthStore((state) => state.initialize);
  const syncFromStorage = useAuthStore((state) => state.syncFromStorage);
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const {
    activeWorkspace,
    preferredWorkspaceByUser,
    setActiveWorkspace,
    isDarkMode,
    locale,
  } = useUIStore();

  const resolvedHomePath = React.useMemo(
    () => resolveHomePath(user, location.pathname, activeWorkspace, preferredWorkspaceByUser),
    [user, location.pathname, activeWorkspace, preferredWorkspaceByUser]
  );

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

  React.useEffect(() => {
    void i18n.changeLanguage(locale);
  }, [locale]);

  React.useEffect(() => {
    window.document.title = buildDocumentTitle(location.pathname, i18n.t.bind(i18n));
  }, [locale, location.pathname]);

  React.useEffect(() => {
    const handler = () => {
      const nextState = location.pathname === '/login'
        ? { expired: true }
        : { from: location.pathname, expired: true };
      navigate('/login', { replace: true, state: nextState });
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, handler);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler);
  }, [location.pathname, navigate]);

  React.useEffect(() => {
    if (!user) {
      setActiveWorkspace(null);
      return;
    }

    const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
    const nextWorkspace = resolveActiveWorkspace({
      user,
      pathname: location.pathname,
      activeWorkspace,
      preferredWorkspace,
    });

    if (nextWorkspace && nextWorkspace !== activeWorkspace) {
      setActiveWorkspace(nextWorkspace, user);
    }
  }, [user, location.pathname, activeWorkspace, preferredWorkspaceByUser, setActiveWorkspace]);

  if (status === 'idle' || status === 'loading') {
    return <BootScreen />;
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={status === 'authenticated' && user ? <Navigate to={resolvedHomePath} replace /> : withSuspense(<Login />)}
      />
      <Route
        path="/register"
        element={status === 'authenticated' && user ? <Navigate to={resolvedHomePath} replace /> : withSuspense(<Register />)}
      />
      <Route path="/account-action/:token" element={withSuspense(<AccountActionPage />)} />

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
          path="assessments"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<StudentAssessmentsPage />)}</RequireCapability>
          }
        />
        <Route
          path="assessments/attempts/:attemptId"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<StudentAssessmentAttemptPage />)}</RequireCapability>
          }
        />
        <Route
          path="assessments/attempts/:attemptId/result"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<StudentAssessmentResultPage />)}</RequireCapability>
          }
        />
        <Route
          path="errors"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<ErrorsPage />)}</RequireCapability>
          }
        />
        <Route
          path="history"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<HistoryPage />)}</RequireCapability>
          }
        />

        <Route
          path="teacher/workspace"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherWorkspacePage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassesPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes/new"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/assessments"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherAssessmentsPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/assessments/new"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherAssessmentEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/assessments/:paperId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherAssessmentEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/assessments/publishes/:publishId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherAssessmentPublishDetailPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/assessments/attempts/:attemptId/result"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherAssessmentAttemptResultPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes/:classId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassDetailPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/classes/:classId/edit"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherClassEditorPage />)}</RequireCapability>
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
          path="teacher/diagnosis-template-drafts/:draftId"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherTemplateDraftEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-pairs"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalPairsPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-pairs/new"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalPairEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-pairs/:lexicalPairId/edit"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalPairEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="teacher/lexical-pairs/imports"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherLexicalPairImportsPage />)}</RequireCapability>
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
          path="admin/lexical-pairs"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminLexicalPairsPage />)}</RequireCapability>
          }
        />
        <Route
          path="admin/lexical-pairs/new"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminLexicalPairEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="admin/lexical-pairs/:lexicalPairId/edit"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminLexicalPairEditorPage />)}</RequireCapability>
          }
        />
        <Route
          path="admin/lexical-pairs/imports"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminLexicalPairImportsPage />)}</RequireCapability>
          }
        />

        <Route
          path="admin/users"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminUsersPage />)}</RequireCapability>
          }
        />

        <Route path="teacher" element={<Navigate to="/teacher/workspace" replace />} />
        <Route path="monitor" element={<Navigate to="/teacher/interventions" replace />} />
        <Route path="settings" element={withSuspense(<SettingsPage />)} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
