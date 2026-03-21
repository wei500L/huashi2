import React from 'react';
import { AlertTriangle, Home, RefreshCcw } from 'lucide-react';
import { useLocation } from 'react-router-dom';

type ErrorBoundaryVariant = 'fullscreen' | 'panel';

type BoundaryProps = {
  children: React.ReactNode;
  resetKey: string;
  homePath: string;
  homeLabel: string;
  variant: ErrorBoundaryVariant;
  title?: string;
  description?: string;
};

type BoundaryState = {
  hasError: boolean;
  error: Error | null;
};

class ErrorBoundaryRoot extends React.Component<BoundaryProps, BoundaryState> {
  state: BoundaryState = {
    hasError: false,
    error: null,
  };

  static getDerivedStateFromError(error: Error): BoundaryState {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('app_error_boundary_caught', error, errorInfo);
  }

  componentDidUpdate(prevProps: BoundaryProps) {
    if (prevProps.resetKey !== this.props.resetKey && this.state.hasError) {
      this.setState({
        hasError: false,
        error: null,
      });
    }
  }

  private reloadPage = () => {
    window.location.reload();
  };

  private navigateHome = () => {
    window.location.assign(this.props.homePath);
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    const title = this.props.title ?? '页面遇到了未处理错误';
    const description = this.props.description ?? '请重新加载页面；如果问题持续出现，请稍后重试。';
    const shellClassName =
      this.props.variant === 'fullscreen'
        ? 'min-h-screen bg-background relative overflow-hidden'
        : 'relative';
    const backdrop =
      this.props.variant === 'fullscreen' ? (
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.16),transparent_38%),radial-gradient(circle_at_bottom_right,rgba(14,165,233,0.12),transparent_32%)]" />
      ) : null;
    const contentClassName =
      this.props.variant === 'fullscreen'
        ? 'relative z-10 min-h-screen flex items-center justify-center p-6'
        : 'relative z-10 max-w-3xl mx-auto';

    return (
      <div className={shellClassName}>
        {backdrop}
        <div className={contentClassName}>
          <section className="w-full max-w-3xl liquid-glass-panel rounded-[3rem] edge-light p-8 md:p-10">
            <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 text-xs font-black uppercase tracking-[0.24em] text-slate-500 dark:text-white/40">
              <AlertTriangle size={14} className="text-amber-500" />
              runtime fallback
            </div>
            <h2 className="mt-6 text-3xl md:text-4xl font-black tracking-tight text-slate-900 dark:text-white">{title}</h2>
            <p className="mt-4 max-w-2xl text-sm md:text-base leading-7 text-slate-500 dark:text-white/50">{description}</p>
            {this.state.error?.message && (
              <div className="mt-6 rounded-[1.5rem] border border-slate-200/80 dark:border-white/10 bg-white/70 dark:bg-slate-950/40 px-4 py-3 text-xs text-slate-500 dark:text-white/45 break-words">
                {this.state.error.message}
              </div>
            )}
            <div className="mt-8 flex flex-col sm:flex-row gap-3">
              <button
                type="button"
                onClick={this.reloadPage}
                className="inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-3 btn-liquid text-white"
              >
                <RefreshCcw size={16} />
                重新加载页面
              </button>
              <button
                type="button"
                onClick={this.navigateHome}
                className="inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-3 border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 text-slate-700 dark:text-white/75"
              >
                <Home size={16} />
                {this.props.homeLabel}
              </button>
            </div>
          </section>
        </div>
      </div>
    );
  }
}

type RouteErrorBoundaryProps = {
  children: React.ReactNode;
  variant?: ErrorBoundaryVariant;
  title?: string;
  description?: string;
};

export const RouteErrorBoundary: React.FC<RouteErrorBoundaryProps> = ({
  children,
  variant = 'panel',
  title,
  description,
}) => {
  const location = useLocation();
  const homePath = location.pathname === '/login' ? '/login' : '/';
  const homeLabel = location.pathname === '/login' ? '返回登录页' : '返回首页';

  return (
    <ErrorBoundaryRoot
      resetKey={location.pathname}
      homePath={homePath}
      homeLabel={homeLabel}
      variant={variant}
      title={title}
      description={description}
    >
      {children}
    </ErrorBoundaryRoot>
  );
};
