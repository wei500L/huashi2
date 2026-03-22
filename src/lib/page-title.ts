import type { TFunction } from 'i18next';

export function resolveRouteTitle(pathname: string, t: TFunction): string {
  if (pathname === '/login') {
    return t('login.accountLogin');
  }
  if (pathname.startsWith('/teacher/classes')) {
    return t('shell.titles.teacher');
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
  if (pathname.startsWith('/admin/users')) {
    return t('shell.titles.adminUsers');
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

export function buildDocumentTitle(pathname: string, t: TFunction): string {
  const appName = t('common.appName');
  const pageTitle = resolveRouteTitle(pathname, t);
  if (pageTitle === appName) {
    return appName;
  }
  return `${pageTitle} | ${appName}`;
}
