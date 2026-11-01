import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev mode: Vite on http://localhost:5173 proxies API calls to Spring Boot on 8080.
// Production: `npm run build` writes dist/, which Maven copies into Spring Boot's static folder.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/swagger-ui': 'http://localhost:8080',
      '/v3': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1200,
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
