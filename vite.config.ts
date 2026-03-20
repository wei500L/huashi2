import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
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
          if (/node_modules\/(echarts|echarts-for-react|zrender)\//.test(id)) {
            return 'charts-vendor';
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
        },
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端接口预留
        changeOrigin: true,
      },
    },
  },
});
