import React from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation } from 'react-router-dom';
import { FeedbackState } from './FeedbackState';

type ErrorBoundaryVariant = 'fullscreen' | 'panel';

type BoundaryProps = {
  children: React.ReactNode;
  resetKey: string;
  homePath: string;
  homeLabel: string;
  variant: ErrorBoundaryVariant;
  title?: string;
  description?: string;
  impact: string;
  nextStep: string;
  reloadLabel: string;
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
    console.error('app_error_boundary_caught', {
      message: error.message,
      stack: error.stack,
      path: window.location.pathname,
      componentStack: errorInfo.componentStack,
    });
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

    const title = this.props.title ?? '页面暂时没有正常加载';
    const description = this.props.description ?? '系统在渲染当前页面时遇到了异常，已经停止继续执行这部分内容。';
    const shellClassName =
      this.props.variant === 'fullscreen'
        ? 'min-h-screen bg-background relative overflow-hidden'
        : 'relative';
    const backdrop =
      this.props.variant === 'fullscreen' ? (
        <div className="absolute inset-0 bg-background" aria-hidden="true" />
      ) : null;
    const contentClassName =
      this.props.variant === 'fullscreen'
        ? 'relative z-10 min-h-screen flex items-center justify-center p-6'
        : 'relative z-10 max-w-3xl mx-auto';

    return (
      <div className={shellClassName}>
        {backdrop}
        <div className={contentClassName}>
          <FeedbackState
            kind="error"
            className="w-full max-w-3xl surface-panel rounded-xl p-8 md:p-10"
            title={title}
            description={description}
            impact={this.props.impact}
            nextStep={this.props.nextStep}
            primaryAction={{
              label: this.props.reloadLabel,
              onClick: this.reloadPage,
            }}
            secondaryAction={{
              label: this.props.homeLabel,
              onClick: this.navigateHome,
              tone: 'secondary',
            }}
          />
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
  const { t } = useTranslation();
  const location = useLocation();
  const homePath = location.pathname === '/login' ? '/login' : '/';
  const homeLabel = location.pathname === '/login'
    ? t('ui.errorBoundary.backToLogin')
    : t('ui.errorBoundary.backToHome');

  return (
    <ErrorBoundaryRoot
      resetKey={location.pathname}
      homePath={homePath}
      homeLabel={homeLabel}
      variant={variant}
      title={title ?? t('ui.errorBoundary.title')}
      description={description ?? t('ui.errorBoundary.description')}
      impact={t('ui.errorBoundary.safety')}
      nextStep={t('ui.errorBoundary.nextStep')}
      reloadLabel={t('ui.errorBoundary.reload')}
    >
      {children}
    </ErrorBoundaryRoot>
  );
};
