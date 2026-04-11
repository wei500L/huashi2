import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { formatDateTime, roleLabel, sessionActivityLabel, workspaceLabels } from '@/lib/format';
import { authService } from '@/lib/services';
import { useAuthStore, useUIStore } from '@/store';

const SettingsPage: React.FC = () => {
  const { t } = useTranslation();
  const { user, logout } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();

  const sessionQuery = useQuery({
    queryKey: ['auth-session-overview'],
    queryFn: ({ signal }) => authService.getSessionOverview({ signal }),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader eyebrow={t('shell.nav.settings')} title={t('ui.pages.settings.title')} subtitle={t('ui.pages.settings.subtitle')} />

      <div className="grid gap-8 xl:grid-cols-3">
        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <SectionEyebrow>{t('ui.sections.account')}</SectionEyebrow>
          <div className="text-2xl font-black text-slate-900 dark:text-white">{user?.displayName || '--'}</div>
          <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
            <div>{t('ui.fields.username')}：{user?.username || '--'}</div>
            <div>{t('ui.fields.email')}：{user?.email || '--'}</div>
            <div>{t('ui.fields.roles')}：{(user?.roles || []).map((role) => roleLabel(role)).join(' / ') || '--'}</div>
            <div>{t('ui.fields.workspaces')}：{workspaceLabels(user?.capabilities).join(' / ') || '--'}</div>
            <div>{t('ui.fields.lastLogin')}：{formatDateTime(user?.lastLoginAt)}</div>
          </div>
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <SectionEyebrow>{t('ui.sections.organization')}</SectionEyebrow>
          {user?.studentProfile ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>{t('ui.fields.studentNo')}：{user.studentProfile.studentNo}</div>
              <div>{t('ui.fields.grade')}：{user.studentProfile.gradeName}</div>
              <div>{t('ui.fields.englishLevel')}：{user.studentProfile.englishLevel}</div>
              <div>{t('ui.fields.frenchLevel')}：{user.studentProfile.frenchLevel}</div>
              <div>{t('ui.fields.courseStage')}：{user.studentProfile.courseStage}</div>
            </div>
          ) : user?.teacherProfile ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>{t('ui.fields.employeeNo')}：{user.teacherProfile.employeeNo}</div>
              <div>{t('ui.fields.department')}：{user.teacherProfile.department}</div>
              <div>{t('ui.fields.title')}：{user.teacherProfile.title}</div>
            </div>
          ) : (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noProfile')}</div>
          )}
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <SectionEyebrow>{t('ui.sections.session')}</SectionEyebrow>
          {sessionQuery.data ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>{t('ui.fields.activeSession')}：{sessionActivityLabel(sessionQuery.data.hasActiveSession)}</div>
              <div>{t('ui.fields.accessTokenExpiresAt')}：{formatDateTime(sessionQuery.data.accessTokenExpiresAt)}</div>
              <div>{t('ui.fields.refreshIssuedAt')}：{formatDateTime(sessionQuery.data.refreshSessionIssuedAt)}</div>
              <div>{t('ui.fields.refreshExpiresAt')}：{formatDateTime(sessionQuery.data.refreshSessionExpiresAt)}</div>
              <div>{t('ui.fields.userAgentFingerprint')}：{sessionQuery.data.userAgentFingerprint || '--'}</div>
              <div>{t('ui.fields.issuedIpAddress')}：{sessionQuery.data.issuedIpAddress || '--'}</div>
            </div>
          ) : sessionQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingSession')}</div>
          ) : (
            <div className="text-sm text-rose-500">{sessionQuery.error instanceof Error ? sessionQuery.error.message : t('ui.labels.sessionLoadFailed')}</div>
          )}

          <div className="pt-4 flex flex-wrap gap-3">
            <button type="button" onClick={toggleDarkMode} className="rounded-2xl border border-slate-200 px-5 py-3 text-sm dark:border-white/10">
              {isDarkMode ? t('common.actions.lightMode') : t('common.actions.darkMode')}
            </button>
            <button type="button" onClick={() => void logout()} className="btn-liquid px-5 py-3 text-white">
              {t('ui.actions.signOutAllSessions')}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};

export default SettingsPage;
