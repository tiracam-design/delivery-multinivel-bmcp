// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Cliente API Dinámico y Adaptable con Axios & Interceptores HTTP (RBAC)
// ═══════════════════════════════════════════════════════════════════════

import axios from 'axios';

const TOKEN_KEY = 'delivery_jwt_token';
const LEGACY_TOKEN_KEY = 'bmcp_jwt_token';
const USER_KEY = 'delivery_auth_user';

/**
 * Resuelve dinámicamente la URL base del backend para soportar:
 * 1. Variables de entorno de Vite (VITE_API_BASE_URL o VITE_API_URL)
 * 2. Túneles públicos (ngrok, localtunnel, Cloudflare tunnels, pinggy)
 * 3. Redes locales LAN por IP (ej: 192.168.x.x, 172.x.x.x)
 * 4. Entorno de desarrollo local (localhost)
 */
export function getApiBaseUrl({ absolute = false } = {}) {
  const envUrl = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || '').trim();
  if (envUrl) {
    return envUrl.replace(/\/+$/, '');
  }

  if (typeof window !== 'undefined' && window.location) {
    if (absolute) {
      const { protocol, host } = window.location;
      return `${protocol}//${host}/api`;
    }
    return '/api';
  }

  return 'http://localhost:8080/api';
}

/**
 * Resuelve dinámicamente la URL del WebSocket para STOMP / SockJS.
 */
export function getWebSocketUrl(path = '/ws-delivery') {
  const envWs = (import.meta.env.VITE_WS_URL || '').trim();
  if (envWs) {
    return envWs.replace(/\/+$/, '');
  }

  if (typeof window !== 'undefined' && window.location) {
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    return `${wsProtocol}//${window.location.host}${cleanPath}`;
  }

  return `ws://localhost:8080${path.startsWith('/') ? path : `/${path}`}`;
}

/**
 * Construye una URL completa hacia la API a partir de un path relativo o absoluto.
 */
export function buildApiUrl(path = '') {
  if (!path) return getApiBaseUrl();

  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  const base = getApiBaseUrl();
  const cleanPath = path.startsWith('/') ? path : `/${path}`;

  if (base.endsWith('/api') && cleanPath.startsWith('/api/')) {
    return `${base}${cleanPath.substring(4)}`;
  }

  return `${base}${cleanPath}`;
}

/**
 * Obtiene el token JWT actual almacenado en el navegador.
 * @returns {string|null}
 */
export function getAuthToken() {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY) || null;
}

// ═══════════════════════════════════════════════════════════════════════
// INSTANCIA AXIOS GLOBAL CON INTERCEPTORES E2E
// ═══════════════════════════════════════════════════════════════════════

export const apiAxios = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': 'true',
    'Bypass-Tunnel-Reminder': 'true'
  },
  timeout: 15000
});

// Interceptor de Peticiones: Adjunta automáticamente Bearer Token
apiAxios.interceptors.request.use(
  (config) => {
    config.baseURL = getApiBaseUrl();
    const token = getAuthToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor de Respuestas: Manejo global de HTTP 401 y 403
apiAxios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        console.warn('⚠️ [AXIOS-INTERCEPTOR] Sesión expirada o token inválido (HTTP 401). Redirigiendo al login...');
        if (typeof window !== 'undefined') {
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(LEGACY_TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          window.dispatchEvent(new Event('auth-changed'));
        }
      } else if (status === 403) {
        console.warn('⛔ [AXIOS-INTERCEPTOR] Acceso prohibido (HTTP 403 Forbidden). Permisos insuficientes.');
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Cliente Fetch dinámico para compatibilidad total con llamadas fetch
 */
export async function apiFetch(endpoint, options = {}) {
  const url = buildApiUrl(endpoint);
  const token = getAuthToken();

  const headers = {
    'Accept': 'application/json',
    'ngrok-skip-browser-warning': 'true',
    'Bypass-Tunnel-Reminder': 'true',
    ...(options.headers || {})
  };

  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  if (response.status === 401) {
    console.warn('⚠️ [FETCH-INTERCEPTOR] Token expirado o no autorizado (HTTP 401).');
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(LEGACY_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.dispatchEvent(new Event('auth-changed'));
    }
  }

  if (response.status === 403) {
    console.warn('⛔ [FETCH-INTERCEPTOR] Acceso prohibido (HTTP 403 Forbidden).');
  }

  return response;
}

/**
 * Métodos auxiliares HTTP
 */
export const api = {
  get: (endpoint, options = {}) => apiFetch(endpoint, { ...options, method: 'GET' }),

  post: (endpoint, data, options = {}) => apiFetch(endpoint, {
    ...options,
    method: 'POST',
    body: typeof data === 'object' && !(data instanceof FormData) ? JSON.stringify(data) : data
  }),

  put: (endpoint, data, options = {}) => apiFetch(endpoint, {
    ...options,
    method: 'PUT',
    body: typeof data === 'object' && !(data instanceof FormData) ? JSON.stringify(data) : data
  }),

  patch: (endpoint, data, options = {}) => apiFetch(endpoint, {
    ...options,
    method: 'PATCH',
    body: typeof data === 'object' && !(data instanceof FormData) ? JSON.stringify(data) : data
  }),

  delete: (endpoint, options = {}) => apiFetch(endpoint, { ...options, method: 'DELETE' }),

  axios: apiAxios,
  getBaseUrl: getApiBaseUrl,
  getWebSocketUrl,
  buildUrl: buildApiUrl
};

export default api;
