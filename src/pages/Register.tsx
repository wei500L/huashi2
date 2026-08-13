import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { BadgeCheck, KeyRound, RefreshCcw, UserPlus2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { RoundedSelect } from '@/components/common/RoundedSelect';
import { ApiError, getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type { StudentRegistrationContextVO } from '@/lib/contracts';
import { authService } from '@/lib/services';
import { useAuthStore } from '@/store';

type RegisterFormData = {
  classCode: string;
  displayName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  frenchLevel: string;
  courseStage: string;
};

const levelOptions = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
const courseStageOptions = ['FOUNDATION', 'INTERMEDIATE', 'ADVANCED'] as const;
const REGISTRATION_CONTEXT_RESOLVE_DELAY_MS = 350;

type ResolvedRegistrationContext = {
  requestedClassCode: string;
  payload: StudentRegistrationContextVO;
};

const RegisterPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialClassCode = searchParams.get('code') ?? '';
  const { registerStudent, clearError } = useAuthStore();
  const [resolvedContext, setResolvedContext] = React.useState<ResolvedRegistrationContext | null>(null);
  const [contextErrorMessage, setContextErrorMessage] = React.useState<string | null>(null);
  const [submitErrorState, setSubmitErrorState] = React.useState<{ message: string; traceId?: string | null } | null>(null);
  const [isResolvingContext, setIsResolvingContext] = React.useState(false);
  const [contextRefreshNonce, setContextRefreshNonce] = React.useState(0);

  const registerSchema = React.useMemo(() => z.object({
    classCode: z.string().min(1, t('register.validation.inviteCodeRequired')),
    displayName: z.string().min(1, t('register.validation.displayNameRequired')),
    username: z.string().min(1, t('register.validation.usernameRequired')),
    email: z.string().min(1, t('register.validation.emailRequired')).email(t('register.validation.emailInvalid')),
    password: z.string()
      .min(1, t('register.validation.passwordRequired'))
      .min(8, t('register.validation.passwordMin')),
    confirmPassword: z.string().min(1, t('register.validation.confirmPasswordRequired')),
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
    setValue,
    setError,
    clearErrors,
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
      frenchLevel: '',
      courseStage: 'FOUNDATION',
    },
  });

  React.useEffect(() => {
    clearError();
  }, [clearError]);

  const currentClassCode = watch('classCode').trim();
  const frenchLevelValue = watch('frenchLevel');
  const courseStageValue = watch('courseStage');
  const deferredClassCode = React.useDeferredValue(currentClassCode);
  const activeResolvedContext = resolvedContext?.requestedClassCode === currentClassCode ? resolvedContext.payload : null;
  const levelSelectOptions = React.useMemo(
    () => [
      { value: '', label: '--' },
      ...levelOptions.map((level) => ({ value: level, label: t(`register.levelOptions.${level}`) })),
    ],
    [t],
  );
  const courseStageSelectOptions = React.useMemo(
    () => courseStageOptions.map((stage) => ({ value: stage, label: t(`register.courseStageOptions.${stage}`) })),
    [t],
  );
  const tokenExpiryLabel = React.useMemo(() => {
    if (!activeResolvedContext?.registrationTokenExpiresAt) {
      return null;
    }
    return new Intl.DateTimeFormat(i18n.language === 'zh-CN' ? 'zh-CN' : 'en-US', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(activeResolvedContext.registrationTokenExpiresAt));
  }, [activeResolvedContext?.registrationTokenExpiresAt, i18n.language]);

  const triggerRegistrationContextRefresh = React.useCallback(() => {
    React.startTransition(() => {
      setContextRefreshNonce((current) => current + 1);
    });
  }, []);

  const resolveRegistrationContext = React.useEffectEvent(async (classCode: string, signal: AbortSignal) => {
    setIsResolvingContext(true);
    try {
      const payload = await authService.resolveRegistrationContext({ classCode }, { signal });
      if (signal.aborted) {
        return;
      }
      clearErrors('classCode');
      setResolvedContext({ requestedClassCode: classCode, payload });
      setContextErrorMessage(null);
    } catch (error) {
      if (signal.aborted) {
        return;
      }
      const normalizedError = normalizeApiError(error);
      setResolvedContext(null);
      if (normalizedError.code === 'VALIDATION_ERROR') {
        setContextErrorMessage(t('register.contextState.invalid'));
        return;
      }
      if (normalizedError.code === 'RATE_LIMITED') {
        setContextErrorMessage(t('register.contextState.rateLimited'));
        return;
      }
      setContextErrorMessage(getApiErrorMessage(error, t('register.contextState.unavailable')));
    } finally {
      if (!signal.aborted) {
        setIsResolvingContext(false);
      }
    }
  });

  React.useEffect(() => {
    setSubmitErrorState(null);
  }, [currentClassCode]);

  React.useEffect(() => {
    if (!deferredClassCode) {
      clearErrors('classCode');
      setResolvedContext(null);
      setContextErrorMessage(null);
      setIsResolvingContext(false);
      return;
    }

    clearErrors('classCode');
    setResolvedContext((current) => current?.requestedClassCode === deferredClassCode ? current : null);
    setContextErrorMessage(null);

    const abortController = new AbortController();
    const timer = window.setTimeout(() => {
      void resolveRegistrationContext(deferredClassCode, abortController.signal);
    }, REGISTRATION_CONTEXT_RESOLVE_DELAY_MS);

    return () => {
      abortController.abort();
      window.clearTimeout(timer);
    };
  }, [deferredClassCode, contextRefreshNonce, resolveRegistrationContext]);

  const onSubmit = async (values: RegisterFormData) => {
    clearError();
    setSubmitErrorState(null);
    if (!activeResolvedContext) {
      setError('classCode', {
        type: 'manual',
        message: t('register.validation.resolveInviteCodeFirst'),
      });
      triggerRegistrationContextRefresh();
      return;
    }

    try {
      await registerStudent({
        password: values.password,
        frenchLevel: values.frenchLevel,
        courseStage: values.courseStage,
        registrationToken: activeResolvedContext.registrationToken,
        displayName: values.displayName.trim(),
        username: values.username.trim(),
        email: values.email.trim(),
      });
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.code === 'REGISTRATION_CONTEXT_BUSY') {
        setSubmitErrorState({
          message: t('register.feedback.inProgress'),
          traceId: normalizedError.traceId,
        });
        return;
      }
      if (normalizedError.code === 'REGISTRATION_CONTEXT_INVALID') {
        setResolvedContext(null);
        setContextErrorMessage(t('register.contextState.expired'));
        setError('classCode', {
          type: 'manual',
          message: t('register.validation.inviteCodeExpired'),
        });
        triggerRegistrationContextRefresh();
        return;
      }
      if (normalizedError.status === 409 && /username already exists/i.test(normalizedError.message)) {
        setError('username', {
          type: 'server',
          message: t('register.validation.usernameTaken'),
        });
        return;
      }
      if (normalizedError.status === 409 && /email already exists/i.test(normalizedError.message)) {
        setError('email', {
          type: 'server',
          message: t('register.validation.emailTaken'),
        });
        return;
      }
      if (normalizedError instanceof ApiError && normalizedError.code === 'VALIDATION_ERROR' && /registrationToken/i.test(normalizedError.message)) {
        setResolvedContext(null);
        setContextErrorMessage(t('register.contextState.expired'));
        setError('classCode', {
          type: 'manual',
          message: t('register.validation.inviteCodeExpired'),
        });
        triggerRegistrationContextRefresh();
        return;
      }
      setSubmitErrorState({
        message: getApiErrorMessage(error, t('register.feedback.submitFailed')),
        traceId: normalizedError.traceId,
      });
    }
  };

  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-background" aria-hidden="true" />
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4 sm:p-6">
        <div className="grid w-full max-w-7xl min-w-0 items-stretch gap-6 lg:grid-cols-[1fr_0.96fr] lg:gap-8">
          <section className="liquid-glass-panel flex min-h-0 min-w-0 flex-col justify-between rounded-2xl edge-light p-5 sm:rounded-3xl sm:p-8 md:min-h-[720px] md:p-14">
            <div>
              <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 text-xs font-black uppercase tracking-[0.24em] text-slate-500 dark:text-white/40">
                {t('register.badge')}
              </div>
              <h1 className="type-display mt-8 text-slate-900 dark:text-white">
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
              {activeResolvedContext ? (
                <div className="mt-4">
                  <div className="inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
                    <BadgeCheck size={14} />
                    {t('register.contextState.verified')}
                  </div>
                  <div className="mt-4 text-2xl font-black text-slate-900 dark:text-white">{activeResolvedContext.className}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {activeResolvedContext.gradeName}
                  </div>
                  {tokenExpiryLabel && (
                    <div className="mt-3 text-xs uppercase tracking-[0.18em] text-slate-400 dark:text-white/35">
                      {t('register.contextState.validUntil', { time: tokenExpiryLabel })}
                    </div>
                  )}
                </div>
              ) : (
                <div className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/45">
                  {t('register.classPreviewFallback')}
                </div>
              )}

              {isResolvingContext && (
                <div className="mt-4 text-sm text-slate-400 dark:text-white/35">{t('register.contextState.checking')}</div>
              )}

              {contextErrorMessage && deferredClassCode.length > 0 && (
                <div id="register-class-code-context" className="mt-4 rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-300" role="alert">
                  <div>{contextErrorMessage}</div>
                  <button
                    type="button"
                    onClick={triggerRegistrationContextRefresh}
                    className="mt-3 inline-flex items-center gap-2 font-semibold text-amber-700 transition hover:opacity-80 dark:text-amber-300"
                  >
                    <RefreshCcw size={14} />
                    {t('register.contextState.retry')}
                  </button>
                </div>
              )}
            </div>
          </section>

          <section className="liquid-glass min-w-0 self-center rounded-2xl edge-light p-5 sm:rounded-3xl sm:p-8 md:p-10">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('register.accountRegister')}</div>
            <h2 className="type-section-title mt-3 text-slate-900 dark:text-white">{t('register.accountRegisterTitle')}</h2>

            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <label className="block">
                <div className="type-label mb-2 text-slate-700 dark:text-white/70">{t('register.inviteCodeLabel')}</div>
                <input
                  {...register('classCode')}
                  id="register-class-code"
                  aria-invalid={Boolean(errors.classCode || contextErrorMessage)}
                  aria-describedby={errors.classCode ? 'register-class-code-error' : contextErrorMessage ? 'register-class-code-context' : undefined}
                  data-state={activeResolvedContext ? 'success' : undefined}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('register.inviteCodePlaceholder')}
                />
                {errors.classCode && <div id="register-class-code-error" className="form-message form-message-error" role="alert">{errors.classCode.message}</div>}
              </label>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <div className="type-label mb-2 text-slate-700 dark:text-white/70">{t('register.displayNameLabel')}</div>
                  <input
                    {...register('displayName')}
                    id="register-display-name"
                    aria-invalid={Boolean(errors.displayName)}
                    aria-describedby={errors.displayName ? 'register-display-name-error' : undefined}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.displayNamePlaceholder')}
                  />
                  {errors.displayName && <div id="register-display-name-error" className="form-message form-message-error" role="alert">{errors.displayName.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.usernameLabel')}</div>
                  <input
                    {...register('username')}
                    id="register-username"
                    aria-invalid={Boolean(errors.username)}
                    aria-describedby={errors.username ? 'register-username-error' : undefined}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.usernamePlaceholder')}
                  />
                  {errors.username && <div id="register-username-error" className="form-message form-message-error" role="alert">{errors.username.message}</div>}
                </label>
              </div>

              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.emailLabel')}</div>
                <input
                  type="email"
                  {...register('email')}
                  id="register-email"
                  aria-invalid={Boolean(errors.email)}
                  aria-describedby={errors.email ? 'register-email-error' : undefined}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('register.emailPlaceholder')}
                />
                {errors.email && <div id="register-email-error" className="form-message form-message-error" role="alert">{errors.email.message}</div>}
              </label>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.passwordLabel')}</div>
                  <input
                    type="password"
                    {...register('password')}
                    id="register-password"
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? 'register-password-error' : undefined}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.passwordPlaceholder')}
                  />
                  {errors.password && <div id="register-password-error" className="form-message form-message-error" role="alert">{errors.password.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.confirmPasswordLabel')}</div>
                  <input
                    type="password"
                    {...register('confirmPassword')}
                    id="register-confirm-password"
                    aria-invalid={Boolean(errors.confirmPassword)}
                    aria-describedby={errors.confirmPassword ? 'register-confirm-password-error' : undefined}
                    className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                    placeholder={t('register.confirmPasswordPlaceholder')}
                  />
                  {errors.confirmPassword && <div id="register-confirm-password-error" className="form-message form-message-error" role="alert">{errors.confirmPassword.message}</div>}
                </label>
              </div>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.frenchLevelLabel')}</div>
                  <input type="hidden" {...register('frenchLevel')} />
                  <RoundedSelect
                    id="register-french-level"
                    value={frenchLevelValue}
                    options={levelSelectOptions}
                    ariaLabel={t('register.frenchLevelLabel')}
                    ariaDescribedBy={errors.frenchLevel ? 'register-french-level-error' : undefined}
                    validationState={errors.frenchLevel ? 'invalid' : 'default'}
                    onChange={(value) => setValue('frenchLevel', value, { shouldDirty: true, shouldTouch: true, shouldValidate: true })}
                  />
                  {errors.frenchLevel && <div id="register-french-level-error" className="form-message form-message-error" role="alert">{errors.frenchLevel.message}</div>}
                </label>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('register.courseStageLabel')}</div>
                  <input type="hidden" {...register('courseStage')} />
                  <RoundedSelect
                    id="register-course-stage"
                    value={courseStageValue}
                    options={courseStageSelectOptions}
                    ariaLabel={t('register.courseStageLabel')}
                    ariaDescribedBy={errors.courseStage ? 'register-course-stage-error' : undefined}
                    validationState={errors.courseStage ? 'invalid' : 'default'}
                    onChange={(value) => setValue('courseStage', value, { shouldDirty: true, shouldTouch: true, shouldValidate: true })}
                  />
                  {errors.courseStage && <div id="register-course-stage-error" className="form-message form-message-error" role="alert">{errors.courseStage.message}</div>}
                </label>
              </div>

              {!activeResolvedContext && currentClassCode.length > 0 && !isResolvingContext && !contextErrorMessage && (
                <div className="rounded-2xl border border-slate-200/80 bg-white/60 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/55">
                  {t('register.contextState.pending')}
                </div>
              )}

              {submitErrorState && (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500" role="alert">
                  <div>{submitErrorState.message}</div>
                  {submitErrorState.traceId && (
                    <div className="mt-2 text-xs text-rose-400">{t('register.feedback.traceId', { traceId: submitErrorState.traceId })}</div>
                  )}
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting || isResolvingContext || !activeResolvedContext}
                aria-busy={isSubmitting || isResolvingContext}
                className="btn-liquid w-full py-4 text-white disabled:opacity-70"
              >
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
