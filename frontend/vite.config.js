import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Escuchar en 0.0.0.0 para aceptar conexiones de red local (LAN) y túneles externos
    host: true,
    port: 5173,
    // Habilitar CORS para permitir peticiones desde orígenes cruzados y herramientas dev
    cors: true,
    // Permitir acceso desde cualquier host público o túnel (ngrok, localtunnel, Cloudflare, etc.)
    // Evita el error 'Blocked request: The request host is not allowed' de Vite 5.4.12+ / Vite 6
    allowedHosts: true,
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
    // Proxy inverso hacia el backend Spring Boot
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/ws-delivery': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        secure: false,
      }
    }
  },
  preview: {
    host: true,
    port: 4173,
    cors: true,
    allowedHosts: true
  }
});
