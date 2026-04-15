import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { Brain, BookOpen, ShieldCheck } from 'lucide-react';
import { useAuthStore, useUIStore } from '@/store';
import { clearPendingAuthExpired, hasPendingAuthExpired } from '@/lib/session';
import { getPreferredWorkspaceForUser, homePathForWorkspace, resolveActiveWorkspace } from '@/lib/workspaces';

type LoginFormData = {
  usernameOrEmail: string;
  password: string;
};

const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { login, user, error, clearError } = useAuthStore();
  const activeWorkspace = useUIStore((state) => state.activeWorkspace);
  const preferredWorkspaceByUser = useUIStore((state) => state.preferredWorkspaceByUser);
  const routeState = location.state as { from?: string; expired?: boolean; passwordChanged?: boolean } | null;
  const redirectTo = routeState?.from;
  const passwordChanged = Boolean(routeState?.passwordChanged);
  const expired = !passwordChanged && (Boolean(routeState?.expired) || hasPendingAuthExpired());
  const loginSchema = React.useMemo(() => z.object({
    usernameOrEmail: z.string().min(1, t('login.validation.usernameRequired')),
    password: z.string().min(1, t('login.validation.passwordRequired')),
  }), [t]);
  const valuePillars: Array<{
    label: string;
    hint: string;
    icon: React.ComponentType<{ size?: number; className?: string }>;
  }> = React.useMemo(() => [
    {
      label: t('login.valuePillars.diagnosis.label'),
      hint: t('login.valuePillars.diagnosis.hint'),
      icon: Brain,
    },
    {
      label: t('login.valuePillars.content.label'),
      hint: t('login.valuePillars.content.hint'),
      icon: BookOpen,
    },
    {
      label: t('login.valuePillars.interventions.label'),
      hint: t('login.valuePillars.interventions.hint'),
      icon: ShieldCheck,
    },
  ], [t]);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: '',
      password: '',
    },
  });

  React.useEffect(() => {
    if (user) {
      const preferredWorkspace = getPreferredWorkspaceForUser(user, preferredWorkspaceByUser);
      const nextWorkspace = resolveActiveWorkspace({
        user,
        pathname: location.pathname,
        activeWorkspace,
        preferredWorkspace,
      });
      navigate(redirectTo || homePathForWorkspace(nextWorkspace), { replace: true });
    }
  }, [navigate, redirectTo, user, location.pathname, activeWorkspace, preferredWorkspaceByUser]);

  React.useEffect(() => {
    if (expired || passwordChanged) {
      clearPendingAuthExpired();
    }
  }, [expired, passwordChanged]);

  const onSubmit = async (values: LoginFormData) => {
    clearError();
    clearPendingAuthExpired();
    await login(values);
  };

  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.16),transparent_38%),radial-gradient(circle_at_bottom_right,rgba(14,165,233,0.12),transparent_32%)]" />
      <div className="relative z-10 min-h-screen flex items-center justify-center p-6">
        <div className="w-full max-w-6xl grid lg:grid-cols-[1.15fr_0.85fr] gap-8 items-stretch">
          <section className="liquid-glass-panel rounded-[3rem] edge-light p-10 md:p-14 flex flex-col justify-between min-h-[620px]">
            <div>
              <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 text-xs font-black uppercase tracking-[0.24em] text-slate-500 dark:text-white/40">
                {t('login.badge')}
              </div>
              <h1 className="mt-8 text-5xl md:text-6xl font-black tracking-tight text-slate-900 dark:text-white leading-[1.05]">
                {t('login.title')}
              </h1>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-500 dark:text-white/50">
                {t('login.subtitle')}
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-4 mt-10">
              {valuePillars.map((card) => (
                <div
                  key={card.label}
                  className="text-left rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/55 dark:bg-white/5 p-5"
                >
                  <card.icon size={18} className="text-primary mb-4" />
                  <div className="font-black text-slate-900 dark:text-white">{card.label}</div>
                  <div className="text-sm mt-2 text-slate-500 dark:text-white/45 leading-6">{card.hint}</div>
                </div>
              ))}
            </div>
          </section>

          <section className="liquid-glass rounded-[3rem] edge-light p-8 md:p-10 self-center">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">{t('login.accountLogin')}</div>
            <h2 className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{t('login.accountLoginTitle')}</h2>
            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('login.usernameLabel')}</div>
                <input
                  {...register('usernameOrEmail')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('login.usernamePlaceholder')}
                />
                {errors.usernameOrEmail && <div className="mt-2 text-sm text-rose-500">{errors.usernameOrEmail.message}</div>}
              </label>

              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">{t('login.passwordLabel')}</div>
                <input
                  type="password"
                  {...register('password')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder={t('login.passwordPlaceholder')}
                />
                {errors.password && <div className="mt-2 text-sm text-rose-500">{errors.password.message}</div>}
              </label>

              {expired && (
                <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-600 dark:text-amber-400">
                  {t('login.sessionExpired')}
                </div>
              )}

              {passwordChanged && (
                <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-300">
                  {t('login.passwordChanged')}
                </div>
              )}

              {error && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{error}</div>}

              <button type="submit" disabled={isSubmitting} className="btn-liquid w-full py-4 text-white disabled:opacity-70">
                {isSubmitting ? t('login.submitting') : t('login.submit')}
              </button>

              <div className="rounded-2xl border border-slate-200/70 bg-white/55 px-4 py-4 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/50">
                <div>{t('login.registerCtaHint')}</div>
                <Link to="/register" className="mt-2 inline-flex font-semibold text-primary transition hover:opacity-80">
                  {t('login.registerCta')}
                </Link>
              </div>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Login;
