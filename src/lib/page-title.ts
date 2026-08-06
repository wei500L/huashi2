import type { TFunction } from 'i18next';

export function resolveRouteTitle(pathname: string, t: TFunction, search = ''): string {
  if (pathname === '/login') {
    return t('login.accountLogin');
  }
  if (pathname === '/register') {
    return t('register.accountRegister');
  }
  if (pathname === '/research' || pathname.startsWith('/research/')) {
    return t('shell.titles.publicResearch');
  }
  if (pathname.startsWith('/teacher/workspace')) {
    return t('shell.titles.teacherWorkspace');
  }
  if (pathname.startsWith('/teacher/classes')) {
    return t('shell.titles.teacherClasses');
  }
  if (pathname.startsWith('/teacher/research')
    || (pathname.startsWith('/teacher/assessments/') && new URLSearchParams(search).get('context') === 'research')) {
    return t('shell.titles.teacherResearch');
  }
  if (pathname.startsWith('/teacher/assessments')) {
    return t('shell.titles.teacherAssessments');
  }
  if (pathname.startsWith('/teacher/diagnosis-templates')) {
    return t('shell.titles.teacherTemplates');
  }
  if (pathname.startsWith('/teacher/lexical-pairs')) {
    return t('shell.titles.teacherLexicalPairs');
  }
  if (pathname.startsWith('/teacher/lexical-lists')) {
    return t('shell.titles.teacherLexicalLists');
  }
  if (pathname.startsWith('/teacher/interventions')) {
    return t('shell.titles.teacherInterventions');
  }
  if (pathname.startsWith('/student/research')) {
    return t('shell.titles.studentResearch');
  }
  if (pathname.startsWith('/admin/dashboard')) {
    return t('shell.titles.adminDashboard');
  }
  if (pathname.startsWith('/admin/users')) {
    return t('shell.titles.adminUsers');
  }
  if (pathname.startsWith('/admin/audit-logs')) {
    return t('shell.titles.adminAuditLogs');
  }
  if (pathname.startsWith('/admin/lexical-pairs')) {
    return t('shell.titles.adminLexicalPairs');
  }
  if (pathname.startsWith('/admin/config-center')) {
    return t('shell.titles.adminConfigCenter');
  }
  if (pathname.startsWith('/diagnosis')) {
    return t('shell.titles.diagnosis');
  }
  if (pathname.startsWith('/training')) {
    return t('shell.titles.training');
  }
  if (pathname.startsWith('/analytics')) {
    return t('shell.titles.analytics');
  }
  if (pathname.startsWith('/assessments')) {
    return t('shell.titles.assessments');
  }
  if (pathname.startsWith('/errors')) {
    return t('shell.titles.errors');
  }
  if (pathname.startsWith('/history')) {
    return t('shell.titles.history');
  }
  if (pathname.startsWith('/settings')) {
    return t('shell.nav.settings');
  }
  return t('shell.titles.dashboard');
}

export function buildDocumentTitle(pathname: string, t: TFunction, search = ''): string {
  const appName = t('common.appName');
  const pageTitle = resolveRouteTitle(pathname, t, search);
  if (pageTitle === appName) {
    return appName;
  }
  return `${pageTitle} | ${appName}`;
}
