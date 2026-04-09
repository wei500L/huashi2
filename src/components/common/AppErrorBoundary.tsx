import React from 'react';
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
          <FeedbackState
            kind="error"
            className="w-full max-w-3xl liquid-glass-panel edge-light rounded-[3rem] p-8 md:p-10"
            title={title}
            description={description}
            impact="当前页面无法继续完成渲染，但你已经保存的数据不会因此丢失。"
            nextStep="请先重新加载页面；如果仍然出现相同问题，可稍后再试或返回可用页面继续操作。"
            primaryAction={{
              label: '重新加载页面',
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
