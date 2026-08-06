import React, { Suspense } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { AppLayout } from './components/layout';
import { FeedbackState } from './components/common/FeedbackState';
import { RouteSkeleton } from './components/common';
import i18n from './lib/i18n';
import { buildDocumentTitle } from './lib/page-title';
import { AUTH_EXPIRED_EVENT, SESSION_CHANGE_EVENT, hasPendingAuthExpired } from './lib/session';
import { useAuthStore, useUIStore } from './store';
import { userHasCapability } from './lib/format';
import type { Capability } from './lib/contracts';
import {
  getPreferredWorkspaceForUser,
  resolveActiveWorkspace,
  resolveHomePathForUser,
} from './lib/workspaces';

const Login = React.lazy(() => import('./pages/Login'));
const Register = React.lazy(() => import('./pages/Register'));
const AccountActionPage = React.lazy(() => import('./pages/AccountAction'));
const ResearchLandingPage = React.lazy(() => import('./pages/research/landing'));
const ResearchParticipantPage = React.lazy(() => import('./pages/research/index'));
const Dashboard = React.lazy(() => import('./pages/dashboard/index'));
const DiagnosisPage = React.lazy(() => import('./pages/diagnosis/index'));
const TrainingPage = React.lazy(() => import('./pages/training/index'));
const AnalyticsPage = React.lazy(() => import('./pages/analytics/index'));
const ErrorsPage = React.lazy(() => import('./pages/student/Errors'));
const HistoryPage = React.lazy(() => import('./pages/student/History'));
const StudentAssessmentsPage = React.lazy(() => import('./pages/student/Assessments'));
const StudentResearchPage = React.lazy(() => import('./pages/student/Research'));
const StudentAssessmentAttemptPage = React.lazy(() => import('./pages/student/AssessmentAttempt'));
const StudentAssessmentResultPage = React.lazy(() => import('./pages/student/AssessmentResult'));
const SettingsPage = React.lazy(() => import('./pages/student/Settings'));
const TeacherWorkspacePage = React.lazy(() => import('./pages/teacher/Workspace'));
const TeacherClassesPage = React.lazy(() => import('./pages/teacher/Classes'));
const TeacherClassEditorPage = React.lazy(() => import('./pages/teacher/ClassEditor'));
const TeacherAssessmentsPage = React.lazy(() => import('./pages/teacher/Assessments'));
const TeacherResearchAssessmentsPage = React.lazy(() => import('./pages/teacher/ResearchAssessments'));
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
const AdminDashboardPage = React.lazy(() => import('./pages/admin/Dashboard'));
const AdminUsersPage = React.lazy(() => import('./pages/admin/index'));
const AdminAuditLogsPage = React.lazy(() => import('./pages/admin/AuditLogs'));
const AdminConfigCenterPage = React.lazy(() => import('./pages/admin/ConfigCenter'));
const AdminLexicalPairsPage = React.lazy(() => import('./pages/admin/LexicalPairs'));
const AdminLexicalPairEditorPage = React.lazy(() => import('./pages/admin/LexicalPairEditor'));
const AdminLexicalPairImportsPage = React.lazy(() => import('./pages/admin/LexicalPairImports'));

const BootScreen: React.FC = () => <RouteSkeleton />;

const RouteStatusPage: React.FC<{ code: 403 | 404; title: string; description: string }> = ({ code, title, description }) => {
  const navigate = useNavigate();
  const isPermission = code === 403;
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-3xl items-center px-6 py-16">
      <FeedbackState
        kind={isPermission ? 'permission' : 'empty'}
        className="w-full surface-panel"
        eyebrow={`${code} · ${i18n.t(isPermission ? 'ui.routeStatus.permissionEyebrow' : 'ui.routeStatus.notFoundEyebrow')}`}
        title={title}
        description={description}
        impact={i18n.t(isPermission ? 'ui.routeStatus.permissionSafety' : 'ui.routeStatus.notFoundSafety')}
        nextStep={i18n.t(isPermission ? 'ui.routeStatus.permissionNextStep' : 'ui.routeStatus.notFoundNextStep')}
        primaryAction={{
          label: i18n.t('ui.routeStatus.backHome'),
          onClick: () => navigate('/'),
        }}
      />
    </div>
  );
};

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
  const user = useAuthStore((state) => state.user);
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return userHasCapability(user, capability) ? <>{children}</> : (
    <RouteStatusPage
      code={403}
      title="无权访问此页面"
      description="当前账号没有该工作区权限。你可以返回已有权限的工作区继续使用。"
    />
  );
};

const HomeRedirect: React.FC = () => {
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const activeWorkspace = useUIStore((state) => state.activeWorkspace);
  const preferredWorkspaceByUser = useUIStore((state) => state.preferredWorkspaceByUser);
  return (
    <Navigate
      to={resolveHomePathForUser({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspaceByUser,
      })}
      replace
    />
  );
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
    () =>
      resolveHomePathForUser({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspaceByUser,
      }),
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
    window.document.documentElement.lang = locale;
  }, [locale]);

  React.useEffect(() => {
    window.document.title = buildDocumentTitle(location.pathname, i18n.t.bind(i18n), location.search);
  }, [locale, location.pathname, location.search]);

  React.useEffect(() => {
    const handler = () => {
      if (location.pathname === '/research' || location.pathname.startsWith('/research/')) {
        return;
      }
      const routeState = location.state as { expired?: boolean; from?: string; passwordChanged?: boolean } | null;
      if (location.pathname === '/login' && routeState?.passwordChanged) {
        return;
      }
      const nextState = location.pathname === '/login'
        ? { expired: true }
        : { from: location.pathname, expired: true };
      navigate('/login', { replace: true, state: nextState });
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, handler);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler);
  }, [location.pathname, location.state, navigate]);

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
      <Route path="/research" element={withSuspense(<ResearchLandingPage />)} />
      <Route path="/research/:releaseCode" element={withSuspense(<ResearchParticipantPage />)} />

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
          path="student/research"
          element={
            <RequireCapability capability="STUDENT_WORKSPACE">{withSuspense(<StudentResearchPage />)}</RequireCapability>
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
          path="teacher/research"
          element={
            <RequireCapability capability="TEACHING_WORKSPACE">{withSuspense(<TeacherResearchAssessmentsPage />)}</RequireCapability>
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
          path="admin/dashboard"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminDashboardPage />)}</RequireCapability>
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
        <Route
          path="admin/audit-logs"
          element={
            <RequireCapability capability="ADMIN_CONSOLE">{withSuspense(<AdminAuditLogsPage />)}</RequireCapability>
          }
        />
        <Route path="admin" element={<Navigate to="/admin/dashboard" replace />} />

        <Route path="teacher" element={<Navigate to="/teacher/workspace" replace />} />
        <Route path="monitor" element={<Navigate to="/teacher/interventions" replace />} />
        <Route path="settings" element={withSuspense(<SettingsPage />)} />
        <Route path="*" element={<RouteStatusPage code={404} title="页面不存在" description="链接可能已失效，或地址输入有误。" />} />
      </Route>

      <Route path="*" element={<RouteStatusPage code={404} title="页面不存在" description="链接可能已失效，或地址输入有误。" />} />
    </Routes>
  );
};

export default App;
