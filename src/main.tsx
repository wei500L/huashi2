import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import { RouteErrorBoundary } from './components/common/AppErrorBoundary';
import { normalizeApiError } from './lib/api';
import { setCustomCursorEnabled } from './lib/cursor';
import './lib/i18n';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        const normalizedError = normalizeApiError(error);
        if ([401, 403, 404, 409, 429].includes(normalizedError.status)) {
          return false;
        }
        return failureCount < 1;
      },
      refetchOnWindowFocus: false,
    },
  },
});

setCustomCursorEnabled(false);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <RouteErrorBoundary
          variant="fullscreen"
          title="应用加载失败"
          description="当前页面未能完成初始化。请重新加载；如果问题持续存在，先回到登录页或首页再重试。"
        >
          <App />
        </RouteErrorBoundary>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
