import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080';
  const usePolling = env.VITE_USE_POLLING === 'true';
  const pollingInterval = Number(env.VITE_POLLING_INTERVAL || '300');
  const watch = usePolling
    ? {
        usePolling: true,
        interval: Number.isFinite(pollingInterval) ? pollingInterval : 300,
      }
    : undefined;

  return {
    plugins: [
      react(),
      mode === 'analyze'
        ? visualizer({
            filename: 'dist/bundle-report.html',
            gzipSize: true,
            brotliSize: true,
            open: false,
          })
        : null,
    ].filter(Boolean),
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return;
            }
            if (/node_modules\/zrender\//.test(id)) {
              return 'chart-renderer';
            }
            if (/node_modules\/echarts\//.test(id)) {
              return 'chart-engine';
            }
            if (/node_modules\/(@tanstack\/react-query|react-router|react-router-dom|history)\//.test(id)) {
              return 'app-vendor';
            }
            if (/node_modules\/(framer-motion|lucide-react)\//.test(id)) {
              return 'ui-vendor';
            }
            if (/node_modules\/(react|react-dom|scheduler|loose-envify|object-assign|js-tokens)\//.test(id)) {
              return 'react-vendor';
            }
            return 'vendor';
          },
        },
      },
    },
    server: {
      host: '0.0.0.0',
      port: 3000,
      watch,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: './src/test/setup.ts',
    },
  };
});
