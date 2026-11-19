import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Server-only setting: keep browser API requests on the frontend's own origin.
  const env = loadEnv(mode, __dirname, 'DEV_API_PROXY_TARGET')
  const backendTarget = env.DEV_API_PROXY_TARGET?.trim() || 'http://127.0.0.1:8080'

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: backendTarget,
          // Match Nginx: preserve the frontend Host so same-origin POSTs stay same-origin in Spring.
          changeOrigin: false,
        },
        '/uploads': {
          target: backendTarget,
          changeOrigin: false,
        },
      },
    },
  }
})
