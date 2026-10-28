// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Pantalla de Inicio de Sesión Interactiva con Parallax y Micro-interacciones
// ═══════════════════════════════════════════════════════════════════════

import React, { useState } from 'react';
import { login } from '../../utils/auth.js';
import ThemeToggle from '../../components/ThemeToggle.jsx';
import RegisterModal from './RegisterModal.jsx';
import GoogleAuthModal from './GoogleAuthModal.jsx';
import InteractiveMapBackground from './components/InteractiveMapBackground.jsx';

export default function LoginPage({ onLoginSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  // Micro-interacción: Estado de foco del formulario para resplandor ambiental interactivo
  const [focusedField, setFocusedField] = useState(null); // 'email' | 'password' | null

  // Modales
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isGoogleOpen, setIsGoogleOpen] = useState(false);

  const handleManualSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setErrorMessage('Por favor ingresa tu usuario/correo y contraseña.');
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const user = await login(email, password);
      if (onLoginSuccess) {
        onLoginSuccess(user);
      }
    } catch (err) {
      setErrorMessage(err.message || 'Error al autenticar credenciales.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAuthSuccess = (user) => {
    if (onLoginSuccess) {
      onLoginSuccess(user);
    }
  };

  // Resplandor ambiental y borde dinámico según el campo enfocado
  const getCardDynamicGlow = () => {
    if (focusedField === 'email') {
      return 'border-amber-500/60 dark:border-amber-500/60 shadow-[0_0_55px_-10px_rgba(245,158,11,0.35)] ring-1 ring-amber-500/40';
    }
    if (focusedField === 'password') {
      return 'border-emerald-500/60 dark:border-emerald-500/60 shadow-[0_0_55px_-10px_rgba(16,185,129,0.35)] ring-1 ring-emerald-500/40';
    }
    return 'border-slate-700/60 dark:border-slate-800 border-slate-200/90 shadow-2xl';
  };

  return (
    <div className="relative min-h-screen flex flex-col justify-between overflow-hidden bg-slate-950 dark:bg-slate-950 bg-slate-50 text-slate-100 dark:text-slate-100 text-slate-900 transition-colors duration-500">
      
      {/* ── 1. FONDO DE MAPA INTERACTIVO (PARALLAX + NODOS GPS + RADAR) ──────── */}
      <InteractiveMapBackground activeField={focusedField} />

      {/* ── 2. BARRA SUPERIOR CON SWITCHER DE TEMA ──────────────────────────── */}
      <header className="relative z-10 w-full px-4 sm:px-8 py-4 flex items-center justify-between max-w-7xl mx-auto">
        {/* Identificador Geográfico de Tulcán */}
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-amber-500 via-rose-500 to-emerald-500 flex items-center justify-center font-black text-white text-base shadow-lg shadow-amber-500/25 animate-floatSlow">
            ⚡
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-xs sm:text-sm text-slate-900 dark:text-white tracking-tight block">
                Delivery Multinivel BMCP
              </span>
              <span className="hidden sm:inline-block px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
                Tulcán 🇪🇨
              </span>
            </div>
            <span className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">
              Geolocalización PostGIS • Coordenadas: 0.8116° N, -77.7172° W
            </span>
          </div>
        </div>

        {/* Conmutador de Tema (Modo Oscuro 🌙 / Modo Claro ☀️) */}
        <div className="flex items-center gap-3">
          <ThemeToggle />
        </div>
      </header>

      {/* ── 3. CONTENEDOR PRINCIPAL / TARJETA CON STATE-DRIVEN UX ─────────────── */}
      <main className="relative z-10 flex-1 flex items-center justify-center px-4 py-8 sm:py-12">
        <div className="w-full max-w-md">
          
          {/* Tarjeta de Login Glassmorphic con Resplandor Dinámico */}
          <div
            className={`bg-slate-900/85 dark:bg-slate-900/85 bg-white/90 backdrop-blur-2xl py-8 px-6 sm:px-10 rounded-3xl border transition-all duration-500 ${getCardDynamicGlow()}`}
          >
            
            {/* Encabezado del Formulario */}
            <div className="text-center mb-6">
              <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-amber-500 via-rose-500 to-emerald-500 shadow-xl shadow-amber-500/25 mb-3 group hover:rotate-6 transition-transform duration-300">
                <span className="text-2xl font-black text-white">⚡</span>
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
                Ingreso al Sistema
              </h1>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Plataforma Geográfica & Logística Operativa Tulcán ($ USD)
              </p>

              {/* Indicador de Estado Dinámico de Micro-interacción */}
              <div className="mt-2 h-5 flex items-center justify-center">
                {focusedField === 'email' && (
                  <span className="text-[11px] font-semibold text-amber-500 dark:text-amber-400 flex items-center gap-1.5 animate-fadeIn">
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
                    Identificación de usuario o correo de Tulcán
                  </span>
                )}
                {focusedField === 'password' && (
                  <span className="text-[11px] font-semibold text-emerald-500 dark:text-emerald-400 flex items-center gap-1.5 animate-fadeIn">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    Cifrado HMAC-SHA256 • Conexión protegida
                  </span>
                )}
                {!focusedField && (
                  <span className="text-[11px] text-slate-400 dark:text-slate-500">
                    Ingresa tus credenciales autorizadas
                  </span>
                )}
              </div>
            </div>

            {/* Alerta de Error con Animación Shake */}
            {errorMessage && (
              <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-300 text-xs flex items-start gap-2.5 animate-shake">
                <span className="text-base">⚠️</span>
                <div className="flex-1">
                  <strong className="block font-bold">Error de Autenticación</strong>
                  <span>{errorMessage}</span>
                </div>
              </div>
            )}

            {/* Formulario Limpio: Correo/Usuario + Contraseña */}
            <form className="space-y-4" onSubmit={handleManualSubmit}>
              
              {/* Campo Usuario o Correo */}
              <div>
                <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1.5">
                  Correo Electrónico o Usuario
                </label>
                <div className="relative group">
                  <input
                    type="text"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onFocus={() => setFocusedField('email')}
                    onBlur={() => setFocusedField(null)}
                    placeholder="ej. valeria@tulcan.ec, admin, carlos..."
                    disabled={isLoading}
                    className="w-full pl-10 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-300 group-hover:border-slate-400 dark:group-hover:border-slate-600"
                  />
                  <span className="absolute left-3.5 top-3.5 text-slate-400 text-sm pointer-events-none transition-transform duration-300 group-focus-within:scale-110">
                    👤
                  </span>
                </div>
              </div>

              {/* Campo Contraseña */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-[11px] font-extrabold uppercase tracking-wider text-slate-700 dark:text-slate-300">
                    Contraseña
                  </label>
                </div>
                <div className="relative group">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setFocusedField('password')}
                    onBlur={() => setFocusedField(null)}
                    placeholder="Ingresa tu contraseña"
                    disabled={isLoading}
                    className="w-full pl-10 pr-16 py-3 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all duration-300 group-hover:border-slate-400 dark:group-hover:border-slate-600"
                  />
                  <span className="absolute left-3.5 top-3.5 text-slate-400 text-sm pointer-events-none transition-transform duration-300 group-focus-within:scale-110">
                    🔒
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-3 text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-white text-xs px-1.5 py-0.5 rounded font-semibold transition-colors"
                  >
                    {showPassword ? 'Ocultar' : 'Ver'}
                  </button>
                </div>
              </div>

              {/* Botón Principal con Efecto Shimmer y Micro-animación */}
              <button
                type="submit"
                disabled={isLoading}
                className="relative overflow-hidden w-full mt-2 py-3.5 px-4 rounded-xl text-sm font-black text-slate-950 bg-gradient-to-r from-amber-400 via-amber-500 to-emerald-400 hover:from-amber-300 hover:to-emerald-300 transition-all duration-300 shadow-lg shadow-amber-500/25 active:scale-[0.98] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60 group"
              >
                {/* Reflejo Shimmer Animado */}
                <span className="absolute inset-0 w-1/2 h-full bg-white/20 skew-x-12 -translate-x-full group-hover:animate-shimmer pointer-events-none" />

                {isLoading ? (
                  <div className="flex items-center gap-2.5">
                    <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                    <span>Verificando credenciales con Spring Boot...</span>
                  </div>
                ) : (
                  <>
                    <span className="tracking-wide">Ingresar al Sistema</span>
                    <span className="transition-transform duration-300 group-hover:translate-x-1">➔</span>
                  </>
                )}
              </button>
            </form>

            {/* Separador */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-200 dark:border-slate-800"></div>
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white dark:bg-slate-900 px-3 text-slate-400 font-semibold tracking-wider text-[11px]">
                  O continúa con
                </span>
              </div>
            </div>

            {/* Botón Continuar con Google */}
            <button
              type="button"
              disabled={isLoading}
              onClick={() => setIsGoogleOpen(true)}
              className="w-full py-3 px-4 rounded-xl text-xs sm:text-sm font-bold text-slate-700 dark:text-slate-200 bg-slate-50 dark:bg-slate-800/80 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-300 dark:border-slate-700 hover:border-amber-500/40 dark:hover:border-amber-500/40 transition-all duration-300 shadow-sm hover:shadow-md active:scale-[0.98] flex items-center justify-center gap-3 group cursor-pointer"
            >
              <svg className="w-4 h-4 flex-shrink-0 transition-transform duration-300 group-hover:scale-110" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              <span>Continuar con Google</span>
            </button>

            {/* Enlace al Modal de Registro */}
            <div className="mt-6 pt-5 border-t border-slate-200 dark:border-slate-800 text-center text-xs text-slate-500 dark:text-slate-400">
              ¿No tienes una cuenta?{' '}
              <button
                type="button"
                onClick={() => setIsRegisterOpen(true)}
                className="font-extrabold text-amber-500 dark:text-amber-400 hover:text-amber-600 dark:hover:text-amber-300 underline underline-offset-2 transition-colors cursor-pointer"
              >
                Regístrate aquí
              </button>
            </div>

          </div>

          {/* Sellos de Seguridad en el pie del formulario */}
          <div className="mt-6 flex items-center justify-between px-2 text-[11px] text-slate-500 dark:text-slate-400 font-mono">
            <span className="flex items-center gap-1.5 text-emerald-500 dark:text-emerald-400 font-semibold">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              JWT Bearer 256-bit
            </span>
            <span>Spring Security 6.x</span>
            <span>PostGIS + Redis</span>
          </div>

        </div>
      </main>

      {/* ── 4. FOOTER INSTITUCIONAL TULCÁN ──────────────────────────────────── */}
      <footer className="relative z-10 py-3 text-center text-[11px] text-slate-500 dark:text-slate-500">
        Delivery Multinivel BMCP • Tulcán, Carchi, Ecuador • Moneda Oficial: $ USD
      </footer>

      {/* ── 5. MODALES DE REGISTRO INTENCIONAL Y GOOGLE OAUTH ───────────────── */}
      <RegisterModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        onRegisterSuccess={handleAuthSuccess}
      />

      <GoogleAuthModal
        isOpen={isGoogleOpen}
        onClose={() => setIsGoogleOpen(false)}
        onLoginSuccess={handleAuthSuccess}
      />

    </div>
  );
}
