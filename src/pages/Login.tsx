import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { ArrowRight, BookOpen, Brain, ShieldCheck } from 'lucide-react';
import { useAuthStore, useUIStore } from '@/store';
import { clearPendingAuthExpired, hasPendingAuthExpired } from '@/lib/session';
import { resolveHomePathForUser } from '@/lib/workspaces';

type LoginFormData = {
  usernameOrEmail: string;
  password: string;
};

const TopographyBackdrop: React.FC = () => (
  <svg
    className="auth-topography"
    viewBox="0 0 1440 900"
    preserveAspectRatio="xMidYMid slice"
    aria-hidden="true"
  >
    <g className="auth-topography-lines">
      <path d="M-80 178C83 53 252 43 374 103c138 68 191 13 324-25 164-47 297 34 416 8 109-24 208-108 390-64" />
      <path d="M-89 235C69 119 244 103 362 157c144 66 203 25 337-17 161-51 291 22 417 0 123-21 218-103 395-64" />
      <path d="M-95 297C61 183 226 169 350 215c151 56 214 31 350-11 158-49 283 11 418-4 138-15 234-91 403-51" />
      <path d="M-92 359C58 260 211 238 337 276c157 47 229 38 367-4 154-47 277-1 419-8 151-8 247-77 409-35" />
      <path d="M-75 425c142-88 284-111 405-82 167 39 243 43 380 5 153-42 275-12 421-12 160 0 256-60 412-17" />
      <path d="M-48 496c127-78 263-102 376-80 173 34 254 48 390 14 151-37 274-20 421-12 166 9 260-42 414 0" />
      <path d="M-21 570c117-66 247-89 353-73 178 27 264 48 396 21 150-31 273-25 421-9 169 18 261-22 416 19" />
      <path d="M7 647c107-54 230-74 330-64 181 19 273 45 402 26 147-22 272-27 420-2 171 28 261-1 418 38" />
      <path d="M37 726c99-42 216-56 310-50 182 10 279 38 407 28 144-12 269-23 416 12 173 40 261 21 418 57" />
      <path d="M68 807c94-31 206-39 296-35 180 7 282 30 411 29 140-1 264-15 408 31 171 55 257 43 413 75" />
      <path d="M850 143c44-65 107-80 156-55 55 28 43 85 103 103 70 21 108-46 175-22 69 25 76 104 138 128" />
      <path d="M876 164c34-47 77-59 112-40 42 24 35 70 83 87 61 23 99-31 153-10 54 21 63 80 111 105" />
    </g>
  </svg>
);

const LanguageFocus: React.FC = () => (
  <div className="auth-language-focus" aria-label="English, Français, 中文">
    <span className="auth-language-word auth-language-word-en" lang="en">English</span>
    <span className="auth-language-bridge" aria-hidden="true"><ArrowRight size={15} /></span>
    <span className="auth-language-word auth-language-word-fr" lang="fr">Français</span>
    <span className="auth-language-divider" aria-hidden="true" />
    <span className="auth-language-word auth-language-word-zh" lang="zh-CN">中文</span>
  </div>
);

const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { login, user, error, clearError } = useAuthStore();
  const activeWorkspace = useUIStore((state) => state.activeWorkspace);
  const preferredWorkspaceByUser = useUIStore((state) => state.preferredWorkspaceByUser);
  const routeState = location.state as { from?: string; expired?: boolean; passwordChanged?: boolean } | null;
  const redirectTo = routeState?.from;
  const resolvedRedirectTo = React.useMemo(() => {
    if (!redirectTo || redirectTo === '/login' || redirectTo === '/register' || redirectTo.startsWith('/settings')) {
      return null;
    }
    return redirectTo;
  }, [redirectTo]);
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
      navigate(
        resolvedRedirectTo
          || resolveHomePathForUser({
            user,
            pathname: location.pathname,
            activeWorkspace,
            preferredWorkspaceByUser,
          }),
        { replace: true }
      );
    }
  }, [navigate, resolvedRedirectTo, user, location.pathname, activeWorkspace, preferredWorkspaceByUser]);

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
    <main className="auth-entry-shell min-w-0">
      <TopographyBackdrop />
      <div className="auth-entry-frame min-w-0">
        <section className="auth-brand-panel min-w-0" aria-labelledby="login-brand-title">
          <div className="min-w-0">
            <div className="auth-brand-lockup min-w-0">
              <span className="auth-brand-mark" aria-hidden="true">EF</span>
              <span className="auth-brand-name">{t('login.badge')}</span>
              <span className="auth-brand-rule" aria-hidden="true" />
            </div>

            <LanguageFocus />

            <div className="auth-heading-mask min-w-0">
              <h1 id="login-brand-title" className="auth-brand-title">
                {t('login.title')}
              </h1>
            </div>
            <p className="auth-brand-subtitle">
              {t('login.subtitle')}
            </p>
          </div>

          <div className="auth-workflow" aria-label={t('login.title')}>
            <div className="auth-workflow-label">
              <span>01—03</span>
            </div>
            <div className="auth-workflow-list">
              {valuePillars.map((card) => (
                <div
                  key={card.label}
                  className="auth-workflow-item"
                >
                  <span className="auth-workflow-icon" aria-hidden="true">
                    <card.icon size={18} />
                  </span>
                  <span>
                    <span className="auth-workflow-title">{card.label}</span>
                    <span className="auth-workflow-hint">{card.hint}</span>
                  </span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="auth-form-panel min-w-0" aria-labelledby="login-form-title">
          <div className="auth-form-header min-w-0">
            <div className="auth-form-eyebrow">{t('login.accountLogin')}</div>
            <h2 id="login-form-title" className="auth-form-title">{t('login.accountLoginTitle')}</h2>
            <div className="auth-form-route" aria-hidden="true">
              <span>EN</span><span className="auth-form-route-line" /><span>FR</span>
            </div>
          </div>

          <form className="auth-login-form min-w-0" onSubmit={handleSubmit(onSubmit)}>
            <div className="auth-field-group min-w-0">
              <label htmlFor="login-username" className="auth-field-label">{t('login.usernameLabel')}</label>
              <input
                {...register('usernameOrEmail')}
                id="login-username"
                type="text"
                autoComplete="username"
                aria-invalid={Boolean(errors.usernameOrEmail)}
                aria-describedby={errors.usernameOrEmail ? 'login-username-error' : undefined}
                className="auth-field-input min-w-0"
                placeholder={t('login.usernamePlaceholder')}
              />
              {errors.usernameOrEmail && <div id="login-username-error" className="form-message form-message-error" role="alert">{errors.usernameOrEmail.message}</div>}
            </div>

            <div className="auth-field-group min-w-0">
              <label htmlFor="login-password" className="auth-field-label">{t('login.passwordLabel')}</label>
              <input
                type="password"
                {...register('password')}
                id="login-password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'login-password-error' : undefined}
                className="auth-field-input min-w-0"
                placeholder={t('login.passwordPlaceholder')}
              />
              {errors.password && <div id="login-password-error" className="form-message form-message-error" role="alert">{errors.password.message}</div>}
            </div>

            {expired && (
              <div className="auth-status-message auth-status-warning" role="status">
                {t('login.sessionExpired')}
              </div>
            )}

            {passwordChanged && (
              <div className="auth-status-message auth-status-success" role="status">
                {t('login.passwordChanged')}
              </div>
            )}

            {error && <div className="auth-status-message auth-status-error" role="alert">{error}</div>}

            <button type="submit" disabled={isSubmitting} aria-busy={isSubmitting} className="auth-submit-button">
              <span>{isSubmitting ? t('login.submitting') : t('login.submit')}</span>
              <ArrowRight size={18} aria-hidden="true" />
            </button>

            <div className="auth-register-link">
              <div>{t('login.registerCtaHint')}</div>
              <Link to="/register">
                {t('login.registerCta')}
              </Link>
            </div>
          </form>

          <div className="auth-form-footer" aria-hidden="true">
            <span>EF.Transfer</span>
          </div>
        </section>
      </div>
    </main>
  );
};

export default Login;
