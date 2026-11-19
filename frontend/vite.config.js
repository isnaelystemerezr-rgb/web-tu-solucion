import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El frontend habla con Spring Boot (8080) a través de /api y /uploads.
// El proxy evita CORS en desarrollo y permite que el mismo código funcione en
// cualquier host de preview (no hay localhost escrito a mano en el cliente).
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    // Permite los previews del sandbox (*.e2b.app) y cualquier host de túnel.
    allowedHosts: true,
    proxy: {
      '/api': {
        target: process.env.API_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: process.env.API_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
