import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { BadgeCheck, KeyRound, UserPlus2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { authService } from '@/lib/services';
import { useAuthStore } from '@/store';

type RegisterFormData = {
  classCode: string;
  displayName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  englishLevel: string;
  frenchLevel: string;
  courseStage: string;
};

const levelOptions = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
const courseStageOptions = ['FOUNDATION', 'INTERMEDIATE', 'ADVANCED'] as const;

const RegisterPage: React.FC = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialClassCode = searchParams.get('code') ?? '';
  const { registerStudent, error, clearError } = useAuthStore();

  const registerSchema = React.useMemo(() => z.object({
    classCode: z.string().min(1, t('register.validation.inviteCodeRequired')),
    displayName: z.string().min(1, t('register.validation.displayNameRequired')),
    username: z.string().min(1, t('register.validation.usernameRequired')),
    email: z.string().min(1, t('register.validation.emailRequired')).email(t('register.validation.emailInvalid')),
    password: z.string()
      .min(1, t('register.validation.passwordRequired'))
      .min(8, t('register.validation.passwordMin')),
    confirmPassword: z.string().min(1, t('register.validation.confirmPasswordRequired')),
    englishLevel: z.string().min(1, t('register.validation.englishLevelRequired')),
    frenchLevel: z.string().min(1, t('register.validation.frenchLevelRequired')),
    courseStage: z.string().min(1, t('register.validation.courseStageRequired')),
  }).refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: t('register.validation.passwordMismatch'),
  }), [t]);

  const highlightCards: Array<{
    key: 'joinClass' | 'instantAccess' | 'profileReady';
    icon: React.ComponentType<{ size?: number; className?: string }>;
  }> = React.useMemo(() => [
    { key: 'joinClass', icon: UserPlus2 },
    { key: 'instantAccess', icon: KeyRound },
    { key: 'profileReady', icon: BadgeCheck },
  ], []);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      classCode: initialClassCode,
      displayName: '',
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      englishLevel: '',
      frenchLevel: '',
      courseStage: 'FOUNDATION',
    },
  });

  React.useEffect(() => {
    clearError();
  }, [clearError]);

  const deferredClassCode = React.useDeferredValue(watch('classCode').trim());
  const classContextQuery = useQuery({
    queryKey: ['student-registration-context', deferredClassCode],
    queryFn: ({ signal }) => authService.getRegistrationContext(deferredClassCode, { signal }),
    enabled: deferredClassCode.length > 0,
    retry: false,
  });

  const onSubmit = async (values: RegisterFormData) => {
    clearError();
    const { confirmPassword: _confirmPassword, ...payload } = values;
    await registerStudent({
      ...payload,
      classCode: values.classCode.trim(),
      displayName: values.displayName.trim(),
      username: values.username.trim(),
      email: values.email.trim(),
    });
  };

  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(34,197,94,0.16),transparent_32%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(135deg,rgba(15,23,42,0.03),transparent_45%)]" />
      <div className="relative z-10 min-h-screen flex items-center justify-center p-6">
        <div className="w-full max-w-7xl grid lg:grid-cols-[1fr_0.96fr] gap-8 items-stretch">
          <section className="liquid-glass-panel rounded-[3rem] edge-light p-10 md:p-14 flex flex-col justify-between min-h-[720px]">
            <div>
              <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 text-xs font-black uppercase tracking-[0.24em] text-slate-500 dark:text-white/40">
                {t('register.badge')}
              </div>
              <h1 className="mt-8 text-5xl md:text-6xl font-black tracking-tight text-slate-900 dark:text-white leading-[1.05]">
                {t('register.title')}
              </h1>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-500 dark:text-white/50">
                {t('register.subtitle')}
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-4 mt-10">
              {highlightCards.map((card) => (
                <div
                  key={card.key}
                  className="text-left rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/55 dark:bg-white/5 p-5"
                >
                  <card.icon size={18} className="text-primary mb-4" />
                  <div className="font-black text-slate-900 dark:text-white">{t(`register.highlights.${card.key}.label`)}</div>
                  <div className="text-sm mt-2 text-slate-500 dark:text-white/45 leading-6">{t(`register.highlights.${card.key}.hint`)}</div>
                </div>
              ))}
            </div>

            <div className="rounded-[2rem] border border-slate-200/80 bg-white/60 p-6 dark:border-white/10 dark:bg-white/5">
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
                {t('register.classPreviewLabel')}
              </div>
              {classContextQuery.isSuccess ? (
                <div className="mt-4">
                  <div className="text-2xl font-black text-slate-900 dark:text-white">{classContextQuery.data.className}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {classContextQuery.data.gradeName} · {classContextQuery.data.classCode}
                  </div>
                </div>
              ) : (
                <div className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/45">
                  {t('register.classPreviewFallback')}
                </div>
              )}

              {classContextQuery.isFetching && (
                <div className="mt-4 text-sm text-slate-400 dark:text-white/35">正在校验邀请码...</div>
              )}

              {classContextQuery.isError && deferredClassCode.length > 0 && (
                <div className="mt-4 rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-300">
                  {classContextQuery.error instanceof Error ? classContextQuery.error.message : '邀请码暂不可用'}
                </div>
              )}
            </div>
          </section>

          <section className="liquid-glass rounded-[3rem] edge-light p-8 md:p-10 self-center">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('register.accountRegister')}</div>
            <h2 className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{t('register.accountRegisterTitle')}</h2>

            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.inviteCodeLabel')}</div>
                <input
                  {...register('classCode')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('register.inviteCodePlaceholder')}
                />
                {errors.classCode && <div className="mt-2 text-sm text-rose-500">{errors.classCode.message}</div>}
              </label>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.displayNameLabel')}</div>
                  <input
                    {...register('displayName')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.displayNamePlaceholder')}
                  />
                  {errors.displayName && <div className="mt-2 text-sm text-rose-500">{errors.displayName.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.usernameLabel')}</div>
                  <input
                    {...register('username')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.usernamePlaceholder')}
                  />
                  {errors.username && <div className="mt-2 text-sm text-rose-500">{errors.username.message}</div>}
                </label>
              </div>

              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.emailLabel')}</div>
                <input
                  type="email"
                  {...register('email')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('register.emailPlaceholder')}
                />
                {errors.email && <div className="mt-2 text-sm text-rose-500">{errors.email.message}</div>}
              </label>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.passwordLabel')}</div>
                  <input
                    type="password"
                    {...register('password')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.passwordPlaceholder')}
                  />
                  {errors.password && <div className="mt-2 text-sm text-rose-500">{errors.password.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.confirmPasswordLabel')}</div>
                  <input
                    type="password"
                    {...register('confirmPassword')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.confirmPasswordPlaceholder')}
                  />
                  {errors.confirmPassword && <div className="mt-2 text-sm text-rose-500">{errors.confirmPassword.message}</div>}
                </label>
              </div>

              <div className="grid gap-5 md:grid-cols-3">
                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.englishLevelLabel')}</div>
                  <select
                    {...register('englishLevel')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  >
                    <option value="">--</option>
                    {levelOptions.map((level) => (
                      <option key={level} value={level}>
                        {t(`register.levelOptions.${level}`)}
                      </option>
                    ))}
                  </select>
                  {errors.englishLevel && <div className="mt-2 text-sm text-rose-500">{errors.englishLevel.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.frenchLevelLabel')}</div>
                  <select
                    {...register('frenchLevel')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  >
                    <option value="">--</option>
                    {levelOptions.map((level) => (
                      <option key={level} value={level}>
                        {t(`register.levelOptions.${level}`)}
                      </option>
                    ))}
                  </select>
                  {errors.frenchLevel && <div className="mt-2 text-sm text-rose-500">{errors.frenchLevel.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.courseStageLabel')}</div>
                  <select
                    {...register('courseStage')}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  >
                    {courseStageOptions.map((stage) => (
                      <option key={stage} value={stage}>
                        {t(`register.courseStageOptions.${stage}`)}
                      </option>
                    ))}
                  </select>
                  {errors.courseStage && <div className="mt-2 text-sm text-rose-500">{errors.courseStage.message}</div>}
                </label>
              </div>

              {error && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{error}</div>}

              <button type="submit" disabled={isSubmitting} className="btn-liquid w-full py-4 text-white disabled:opacity-70">
                {isSubmitting ? t('register.submitting') : t('register.submit')}
              </button>

              <div className="rounded-2xl border border-slate-200/70 bg-white/55 px-4 py-4 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/50">
                <div>{t('register.loginCtaHint')}</div>
                <Link to="/login" className="mt-2 inline-flex font-semibold text-primary transition hover:opacity-80">
                  {t('register.loginCta')}
                </Link>
              </div>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
