import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import type { StudentAnalyticsOverviewVO } from '@/lib/contracts';
import { formatDateTime, roleLabel, sessionActivityLabel, userHasCapability, workspaceLabels } from '@/lib/format';
import { authService, studentService } from '@/lib/services';
import {
  requiresStudentProfileCompletion,
  studentCourseStageOptions,
  studentLanguageLevelOptions,
} from '@/lib/student-profile';
import { clearPendingAuthExpired, clearStoredSession, readStoredSession, writeStoredSession } from '@/lib/session';
import { useAuthStore, useUIStore } from '@/store';

type ChangePasswordFormData = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type StudentProfileFormData = {
  gradeName: string;
  frenchLevel: string;
  courseStage: string;
};

const inputClassName =
  'w-full rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 outline-none focus:border-primary/50 dark:border-white/10 dark:bg-slate-950/40';

const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { session, user, logout, syncFromStorage } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();
  const [profileErrorMessage, setProfileErrorMessage] = React.useState<string | null>(null);
  const [profileSuccessMessage, setProfileSuccessMessage] = React.useState<string | null>(null);
  const [passwordErrorMessage, setPasswordErrorMessage] = React.useState<string | null>(null);
  const canManageStudentProfile = userHasCapability(user, 'STUDENT_WORKSPACE');
  const studentProfileRequired = requiresStudentProfileCompletion(user);

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

  const profileSchema = React.useMemo(() => z.object({
    gradeName: z.string()
      .trim()
      .min(1, t('ui.validation.gradeRequired'))
      .max(64, t('ui.validation.gradeMax')),
    frenchLevel: z.string()
      .min(1, t('ui.validation.frenchLevelRequired'))
      .refine((value) => studentLanguageLevelOptions.includes(value as typeof studentLanguageLevelOptions[number]), {
        message: t('ui.validation.frenchLevelRequired'),
      }),
    courseStage: z.string()
      .min(1, t('ui.validation.courseStageRequired'))
      .refine((value) => studentCourseStageOptions.includes(value as typeof studentCourseStageOptions[number]), {
        message: t('ui.validation.courseStageRequired'),
      }),
  }), [t]);

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    reset: resetPassword,
    formState: { errors: passwordErrors, isSubmitting: isPasswordSubmitting },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    reset: resetProfile,
    formState: { errors: profileErrors, isSubmitting: isProfileSubmitting, isDirty: isProfileDirty },
  } = useForm<StudentProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      gradeName: user?.studentProfile?.gradeName || '',
      frenchLevel: user?.studentProfile?.frenchLevel || '',
      courseStage: user?.studentProfile?.courseStage || '',
    },
  });

  React.useEffect(() => {
    resetProfile({
      gradeName: user?.studentProfile?.gradeName || '',
      frenchLevel: user?.studentProfile?.frenchLevel || '',
      courseStage: user?.studentProfile?.courseStage || '',
    });
  }, [
    resetProfile,
    user?.studentProfile?.courseStage,
    user?.studentProfile?.frenchLevel,
    user?.studentProfile?.gradeName,
  ]);

  const sessionQuery = useQuery({
    queryKey: ['auth-session-overview'],
    queryFn: ({ signal }) => authService.getSessionOverview({ signal }),
  });

  const profileMutation = useMutation({
    mutationFn: (payload: StudentProfileFormData) => studentService.updateProfile(payload),
    onSuccess: (studentProfile) => {
      if (!studentProfile) {
        return;
      }

      const currentSession = readStoredSession() || session;
      if (currentSession) {
        writeStoredSession({
          ...currentSession,
          userInfo: {
            ...currentSession.userInfo,
            studentProfile,
          },
        });
      }
      syncFromStorage();
      setProfileErrorMessage(null);
      setProfileSuccessMessage(t('ui.messages.studentProfileSaved'));
      resetProfile({
        gradeName: studentProfile.gradeName || '',
        frenchLevel: studentProfile.frenchLevel || '',
        courseStage: studentProfile.courseStage || '',
      });
      queryClient.setQueryData<StudentAnalyticsOverviewVO | undefined>(['student-overview'], (current) => (
        current ? {
          ...current,
          gradeName: studentProfile.gradeName,
          frenchLevel: studentProfile.frenchLevel,
          latestSnapshot: {
            ...current.latestSnapshot,
            gradeName: studentProfile.gradeName,
            frenchLevel: studentProfile.frenchLevel,
          },
        } : current
      ));
    },
    onError: (error) => {
      setProfileSuccessMessage(null);
      setProfileErrorMessage(getApiErrorMessage(error, t('ui.errors.studentProfileSaveFailed')));
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: (payload: Pick<ChangePasswordFormData, 'currentPassword' | 'newPassword'>) => authService.changePassword(payload),
    onSuccess: async () => {
      await queryClient.cancelQueries();
      resetPassword();
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

  const onProfileSubmit = async (values: StudentProfileFormData) => {
    setProfileErrorMessage(null);
    setProfileSuccessMessage(null);
    await profileMutation.mutateAsync(values);
  };

  const onPasswordSubmit = async (values: ChangePasswordFormData) => {
    setPasswordErrorMessage(null);
    await changePasswordMutation.mutateAsync({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    });
  };

  return (
    <div className="page-stack pb-20">
      <PageHeader eyebrow={t('shell.nav.settings')} title={t('ui.pages.settings.title')} subtitle={t('ui.pages.settings.subtitle')} />

      <div className="grid min-w-0 gap-4 sm:gap-6 xl:grid-cols-3 xl:gap-8">
        <section className="min-w-0 space-y-4 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
          <SectionEyebrow>{t('ui.sections.account')}</SectionEyebrow>
          <div className="break-words text-xl font-black text-slate-900 sm:text-2xl dark:text-white">{user?.displayName || '--'}</div>
          <div className="space-y-2 break-words text-sm text-slate-500 dark:text-white/45">
            <div>{t('ui.fields.username')}：{user?.username || '--'}</div>
            <div>{t('ui.fields.email')}：{user?.email || '--'}</div>
            <div>{t('ui.fields.roles')}：{(user?.roles || []).map((role) => roleLabel(role)).join(' / ') || '--'}</div>
            <div>{t('ui.fields.workspaces')}：{workspaceLabels(user?.capabilities).join(' / ') || '--'}</div>
            <div>{t('ui.fields.lastLogin')}：{formatDateTime(user?.lastLoginAt)}</div>
          </div>
        </section>

        <section className="min-w-0 space-y-4 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
          <SectionEyebrow>{t('ui.sections.organization')}</SectionEyebrow>
          {user?.studentProfile ? (
            <div className="space-y-2 break-words text-sm text-slate-500 dark:text-white/45">
              <div>{t('ui.fields.studentNo')}：{user.studentProfile.studentNo}</div>
              <div>{t('ui.fields.grade')}：{user.studentProfile.gradeName || '--'}</div>
              <div>{t('ui.fields.frenchLevel')}：{user.studentProfile.frenchLevel || '--'}</div>
              <div>{t('ui.fields.courseStage')}：{user.studentProfile.courseStage || '--'}</div>
            </div>
          ) : user?.teacherProfile ? (
            <div className="space-y-2 break-words text-sm text-slate-500 dark:text-white/45">
              <div>{t('ui.fields.employeeNo')}：{user.teacherProfile.employeeNo}</div>
              <div>{t('ui.fields.department')}：{user.teacherProfile.department}</div>
              <div>{t('ui.fields.title')}：{user.teacherProfile.title}</div>
            </div>
          ) : (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noProfile')}</div>
          )}
        </section>

        <section className="min-w-0 space-y-4 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
          <SectionEyebrow>{t('ui.sections.session')}</SectionEyebrow>
          {sessionQuery.data ? (
            <div className="space-y-2 break-words text-sm text-slate-500 dark:text-white/45">
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

          <div className="page-actions pt-4">
            <button type="button" onClick={toggleDarkMode} className="btn-secondary inline-flex items-center justify-center px-5 py-3">
              {isDarkMode ? t('common.actions.lightMode') : t('common.actions.darkMode')}
            </button>
            <button type="button" onClick={() => void logout()} className="btn-secondary inline-flex items-center justify-center px-5 py-3">
              {t('ui.actions.signOutAllSessions')}
            </button>
          </div>
        </section>

        {canManageStudentProfile ? (
          <section className="min-w-0 space-y-5 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8 xl:col-span-3">
            <div className="space-y-2">
              <div className="page-toolbar">
                <SectionEyebrow>{t('ui.sections.studentProfile')}</SectionEyebrow>
                <span className={`rounded-full px-3 py-1 text-xs font-bold ${studentProfileRequired ? 'bg-amber-500/10 text-amber-600 dark:text-amber-300' : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-300'}`}>
                  {studentProfileRequired ? t('ui.labels.studentProfileRequired') : t('ui.labels.studentProfileComplete')}
                </span>
              </div>
              <div className="text-sm leading-7 text-slate-500 dark:text-white/45">{t('ui.labels.studentProfileHint')}</div>
            </div>

            {studentProfileRequired ? (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-4 py-4 text-sm text-amber-700 sm:rounded-3xl sm:px-5 dark:text-amber-200">
                {t('ui.labels.studentProfileIncompleteNotice')}
              </div>
            ) : null}

            <form className="grid min-w-0 gap-4 md:grid-cols-2" onSubmit={handleProfileSubmit(onProfileSubmit)}>
              <label className="block min-w-0">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.grade')}</div>
                <input
                  type="text"
                  {...registerProfile('gradeName')}
                  className={inputClassName}
                  placeholder={t('ui.placeholders.grade')}
                />
                {profileErrors.gradeName ? <div className="mt-2 text-sm text-rose-500">{profileErrors.gradeName.message}</div> : null}
              </label>

              <label className="block min-w-0">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.studentNo')}</div>
                <input type="text" value={user?.studentProfile?.studentNo || '--'} disabled className={`${inputClassName} opacity-70`} />
              </label>

              <label className="block min-w-0">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.frenchLevel')}</div>
                <select {...registerProfile('frenchLevel')} className={inputClassName}>
                  <option value="">--</option>
                  {studentLanguageLevelOptions.map((level) => (
                    <option key={level} value={level}>
                      {t(`register.levelOptions.${level}`)}
                    </option>
                  ))}
                </select>
                {profileErrors.frenchLevel ? <div className="mt-2 text-sm text-rose-500">{profileErrors.frenchLevel.message}</div> : null}
              </label>

              <label className="block min-w-0">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.courseStage')}</div>
                <select {...registerProfile('courseStage')} className={inputClassName}>
                  <option value="">--</option>
                  {studentCourseStageOptions.map((stage) => (
                    <option key={stage} value={stage}>
                      {t(`register.courseStageOptions.${stage}`)}
                    </option>
                  ))}
                </select>
                {profileErrors.courseStage ? <div className="mt-2 text-sm text-rose-500">{profileErrors.courseStage.message}</div> : null}
              </label>

              <div className="page-actions pt-2 md:col-span-2">
                <button type="submit" disabled={isProfileSubmitting || !isProfileDirty} className="btn-liquid inline-flex items-center justify-center px-6 py-3 text-white disabled:opacity-60">
                  {isProfileSubmitting ? t('ui.actions.savingStudentProfile') : t('ui.actions.saveStudentProfile')}
                </button>
                {profileSuccessMessage ? (
                  <div className="w-full rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 sm:w-auto dark:text-emerald-300">
                    {profileSuccessMessage}
                  </div>
                ) : null}
                {profileErrorMessage ? (
                  <div className="w-full rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500 sm:w-auto">
                    {profileErrorMessage}
                  </div>
                ) : null}
              </div>
            </form>
          </section>
        ) : null}

        <section className="min-w-0 space-y-5 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8 xl:col-span-3">
          <div className="space-y-2">
            <SectionEyebrow>{t('ui.sections.security')}</SectionEyebrow>
            <div className="text-sm leading-7 text-slate-500 dark:text-white/45">{t('ui.labels.passwordChangeHint')}</div>
          </div>

          <form className="grid min-w-0 gap-4 md:grid-cols-3" onSubmit={handlePasswordSubmit(onPasswordSubmit)}>
            <label className="block min-w-0">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.currentPassword')}</div>
              <input
                type="password"
                autoComplete="current-password"
                {...registerPassword('currentPassword')}
                className={inputClassName}
              />
              {passwordErrors.currentPassword ? <div className="mt-2 text-sm text-rose-500">{passwordErrors.currentPassword.message}</div> : null}
            </label>

            <label className="block min-w-0">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.newPassword')}</div>
              <input
                type="password"
                autoComplete="new-password"
                {...registerPassword('newPassword')}
                className={inputClassName}
              />
              {passwordErrors.newPassword ? <div className="mt-2 text-sm text-rose-500">{passwordErrors.newPassword.message}</div> : null}
            </label>

            <label className="block min-w-0">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('ui.fields.confirmPassword')}</div>
              <input
                type="password"
                autoComplete="new-password"
                {...registerPassword('confirmPassword')}
                className={inputClassName}
              />
              {passwordErrors.confirmPassword ? <div className="mt-2 text-sm text-rose-500">{passwordErrors.confirmPassword.message}</div> : null}
            </label>

            <div className="page-actions pt-2 md:col-span-3">
              <button type="submit" disabled={isPasswordSubmitting} className="btn-liquid inline-flex items-center justify-center px-6 py-3 text-white disabled:opacity-60">
                {isPasswordSubmitting ? t('ui.actions.changingPassword') : t('ui.actions.changePassword')}
              </button>
              {passwordErrorMessage ? (
                <div className="w-full rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500 sm:w-auto">
                  {passwordErrorMessage}
                </div>
              ) : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  );
};

export default SettingsPage;
