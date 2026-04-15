import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime, roleLabel, sessionActivityLabel, workspaceLabels } from '@/lib/format';
import { authService } from '@/lib/services';
import { clearPendingAuthExpired, clearStoredSession } from '@/lib/session';
import { useAuthStore, useUIStore } from '@/store';

type ChangePasswordFormData = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

const inputClassName =
  'w-full rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 outline-none focus:border-primary/50 dark:border-white/10 dark:bg-slate-950/40';

const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { user, logout, syncFromStorage } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();
  const [passwordErrorMessage, setPasswordErrorMessage] = React.useState<string | null>(null);

  const passwordSchema = React.useMemo(() => z.object({
    currentPassword: z.string().min(1, t('ui.validation.currentPasswordRequired')),
    newPassword: z.string()
      .min(1, t('ui.validation.newPasswordRequired'))
      .min(8, t('ui.validation.newPasswordMin')),
    confirmPassword: z.string().min(1, t('ui.validation.confirmPasswordRequired')),
  }).refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: t('ui.validation.passwordMismatch'),
  }).refine((values) => values.currentPassword !== values.newPassword, {
    path: ['newPassword'],
    message: t('ui.validation.newPasswordDifferent'),
  }), [t]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const sessionQuery = useQuery({
    queryKey: ['auth-session-overview'],
    queryFn: ({ signal }) => authService.getSessionOverview({ signal }),
  });

  const changePasswordMutation = useMutation({
    mutationFn: (payload: Pick<ChangePasswordFormData, 'currentPassword' | 'newPassword'>) => authService.changePassword(payload),
    onSuccess: async () => {
      await queryClient.cancelQueries();
      reset();
      setPasswordErrorMessage(null);
      clearPendingAuthExpired();
      clearStoredSession();
      syncFromStorage();
      navigate('/login', { replace: true, state: { passwordChanged: true } });
    },
    onError: (error) => {
      setPasswordErrorMessage(getApiErrorMessage(error, t('ui.errors.changePasswordFailed')));
    },
  });

  const onSubmit = async (values: ChangePasswordFormData) => {
    setPasswordErrorMessage(null);
    await changePasswordMutation.mutateAsync({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    });
  };

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

        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-5 xl:col-span-2">
          <div className="space-y-2">
            <SectionEyebrow>{t('ui.sections.security')}</SectionEyebrow>
            <div className="text-sm leading-7 text-slate-500 dark:text-white/45">{t('ui.labels.passwordChangeHint')}</div>
          </div>

          <form className="grid gap-4 md:grid-cols-3" onSubmit={handleSubmit(onSubmit)}>
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.currentPassword')}</div>
              <input
                type="password"
                autoComplete="current-password"
                {...register('currentPassword')}
                className={inputClassName}
              />
              {errors.currentPassword && <div className="mt-2 text-sm text-rose-500">{errors.currentPassword.message}</div>}
            </label>

            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.newPassword')}</div>
              <input
                type="password"
                autoComplete="new-password"
                {...register('newPassword')}
                className={inputClassName}
              />
              {errors.newPassword && <div className="mt-2 text-sm text-rose-500">{errors.newPassword.message}</div>}
            </label>

            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.confirmPassword')}</div>
              <input
                type="password"
                autoComplete="new-password"
                {...register('confirmPassword')}
                className={inputClassName}
              />
              {errors.confirmPassword && <div className="mt-2 text-sm text-rose-500">{errors.confirmPassword.message}</div>}
            </label>

            <div className="md:col-span-3 flex flex-wrap items-center gap-3 pt-2">
              <button type="submit" disabled={isSubmitting} className="btn-liquid px-6 py-3 text-white disabled:opacity-60">
                {isSubmitting ? t('ui.actions.changingPassword') : t('ui.actions.changePassword')}
              </button>
              {passwordErrorMessage && (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                  {passwordErrorMessage}
                </div>
              )}
            </div>
          </form>
        </section>
      </div>
    </div>
  );
};

export default SettingsPage;
