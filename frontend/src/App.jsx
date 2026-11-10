// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Carchi, Ecuador
// Aplicación Principal React con RBAC Dinámico, JWT y Soporte Dark/Light
// ═══════════════════════════════════════════════════════════════════════

import React, { useState, useEffect } from 'react';
import LoginPage from './modules/auth/LoginPage.jsx';
import ProtectedRoute from './modules/auth/ProtectedRoute.jsx';
import RestaurantCheckout from './modules/client/pages/RestaurantCheckout.jsx';
import TrackingPage from './modules/client/pages/TrackingPage.jsx';
import ActiveTrip from './modules/driver/pages/ActiveTrip.jsx';
import MerchantKitchen from './modules/merchant/pages/MerchantKitchen.jsx';
import MerchantApprovals from './modules/admin/pages/MerchantApprovals.jsx';
import ThemeToggle from './components/ThemeToggle.jsx';
import BankSettingsSection from './components/BankSettingsSection.jsx';
import { getCurrentUser, getAuthToken, logout, hasAnyRole, getModuleForRole, decodeJwt } from './utils/auth.js';

export default function App() {
  const [currentUser, setCurrentUser] = useState(getCurrentUser());
  const [activeModule, setActiveModule] = useState('client'); // 'client' | 'driver' | 'merchant' | 'admin'
  const [clientSubView, setClientSubView] = useState('checkout'); // 'checkout' | 'tracking'
  const [createdOrder, setCreatedOrder] = useState(() => {
    try {
      const stored = localStorage.getItem('delivery_active_order');
      if (stored) return JSON.parse(stored);
    } catch (e) {
      console.warn('Error reading delivery_active_order:', e);
    }
    return null;
  });
  const [showTokenModal, setShowTokenModal] = useState(false);
  const [showBankModal, setShowBankModal] = useState(false);

  // Escuchar actualizaciones de pedidos para sincronización entre módulos
  useEffect(() => {
    const handleOrderUpdate = (e) => {
      if (e?.detail) {
        setCreatedOrder(e.detail);
      }
    };
    window.addEventListener('order-updated', handleOrderUpdate);
    return () => window.removeEventListener('order-updated', handleOrderUpdate);
  }, []);

  // El tema ya fue aplicado por el script anti-FOUC en index.html.
  // No re-aplicar aquí para evitar flash innecesario.

  // Función de redirección dinámica según el rol del payload JWT
  const redirectToModuleByRole = (user) => {
    if (!user || !user.role) return;
    const targetModule = getModuleForRole(user.role);
    setActiveModule(targetModule);
    if (targetModule === 'client') {
      setClientSubView('checkout');
    }
  };

  // Escuchar cambios de autenticación
  useEffect(() => {
    const handleAuthChange = () => {
      const user = getCurrentUser();
      setCurrentUser(user);
      if (user) {
        redirectToModuleByRole(user);
      }
    };

    window.addEventListener('auth-changed', handleAuthChange);
    return () => window.removeEventListener('auth-changed', handleAuthChange);
  }, []);

  // Al cargar, si ya hay sesión activa, redirigir al módulo según rol
  useEffect(() => {
    if (currentUser) {
      redirectToModuleByRole(currentUser);
    }
  }, []);

  const handleLoginSuccess = (user) => {
    setCurrentUser(user);
    redirectToModuleByRole(user);
  };

  const handleLogout = () => {
    logout();
    setCurrentUser(null);
    setActiveModule('client');
    setCreatedOrder(null);
  };

  const handleOrderConfirmed = (order) => {
    setCreatedOrder(order);
    setClientSubView('tracking');
  };

  // Callback ejecutado por ProtectedRoute cuando un rol no autorizado intenta ingresar
  const handleUnauthorized = (reason) => {
    console.warn(`[RBAC] Acceso no autorizado (${reason}). Reorientando al login...`);
    handleLogout();
  };

  // Si no está autenticado, mostrar pantalla de Login
  if (!currentUser) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  // Verificación de roles para renderizado condicional de pestañas
  const isAdmin = hasAnyRole(currentUser, ['ROLE_ADMIN']);
  const isDriver = hasAnyRole(currentUser, ['ROLE_DRIVER', 'ROLE_ADMIN']);
  const isMerchant = hasAnyRole(currentUser, ['ROLE_MERCHANT', 'ROLE_ADMIN']);
  const isClient = hasAnyRole(currentUser, ['ROLE_CLIENT', 'ROLE_CUSTOMER', 'ROLE_ADMIN']);

  // Mapa de roles autorizados por cada módulo
  const moduleRolesMap = {
    client: ['ROLE_CLIENT', 'ROLE_CUSTOMER', 'ROLE_ADMIN'],
    driver: ['ROLE_DRIVER', 'ROLE_ADMIN'],
    merchant: ['ROLE_MERCHANT', 'ROLE_ADMIN'],
    admin: ['ROLE_ADMIN']
  };

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col font-sans pb-16 md:pb-0 transition-colors duration-300">
      
      {/* ── 1. BARRA SUPERIOR (ESCRITORIO & TABLETS) ─────────────────────────── */}
      <nav className="bg-white/90 dark:bg-slate-900/95 backdrop-blur-md px-4 py-2.5 shadow-md sticky top-0 z-40 border-b border-slate-200 dark:border-slate-800 transition-colors">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          
          {/* Logo y Nombre del Sistema */}
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-amber-500 to-emerald-500 flex items-center justify-center font-black text-white text-base shadow-lg shadow-amber-500/20">
              ⚡
            </div>
            <div>
              <span className="font-extrabold text-sm sm:text-base tracking-tight text-slate-900 dark:text-white block">
                Delivery Multinivel BMCP • Tulcán 🇪🇨
              </span>
              <div className="hidden sm:flex items-center space-x-2 text-[10px] text-slate-500 dark:text-slate-400">
                <span className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-semibold">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping"></span>
                  Sistema Activo
                </span>
                <span>•</span>
                <span>Tulcán, Carchi 🇪🇨</span>
              </div>
            </div>
          </div>

          {/* Selector de Vistas / Módulos en Escritorio (Filtrado estricto por Rol) */}
          <div className="hidden md:flex items-center bg-slate-100 dark:bg-slate-800 p-1 rounded-xl border border-slate-200 dark:border-slate-700 gap-1 text-xs">
            
            {/* 1. Cliente (Visible para Cliente y Administrador) */}
            {isClient && (
              <button
                onClick={() => setActiveModule('client')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all flex items-center gap-1.5 ${
                  activeModule === 'client'
                    ? 'bg-amber-500 text-slate-950 shadow-sm'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>👤 Cliente</span>
              </button>
            )}

            {/* 2. Repartidor (Visible para Repartidor y Administrador) */}
            {isDriver && (
              <button
                onClick={() => setActiveModule('driver')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all flex items-center gap-1.5 ${
                  activeModule === 'driver'
                    ? 'bg-emerald-500 text-slate-950 shadow-sm'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>🛵 Repartidor</span>
              </button>
            )}

            {/* 3. Comercio / Cocina (Visible para Restaurante y Administrador) */}
            {isMerchant && (
              <button
                onClick={() => setActiveModule('merchant')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all flex items-center gap-1.5 ${
                  activeModule === 'merchant'
                    ? 'bg-rose-500 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>🍳 Cocina KDS</span>
              </button>
            )}

            {/* 4. Administrador (Exclusivo para ROLE_ADMIN) */}
            {isAdmin && (
              <button
                onClick={() => setActiveModule('admin')}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all flex items-center gap-1.5 ${
                  activeModule === 'admin'
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>🛡️ Admin GAD</span>
              </button>
            )}
          </div>

          {/* Perfil del Usuario Autenticado, Switcher de Tema y Salir */}
          <div className="flex items-center gap-2 sm:gap-3">
            
            {/* Conmutador de Tema */}
            <ThemeToggle />

            {/* Botón Perfil / Inspector JWT */}
            <button
              onClick={() => setShowTokenModal(true)}
              className="flex items-center gap-2 bg-slate-100 dark:bg-slate-800/80 hover:bg-slate-200 dark:hover:bg-slate-800 border border-slate-300 dark:border-slate-700 px-3 py-1.5 rounded-xl transition-all text-left"
              title="Ver detalles de sesión JWT"
            >
              <div className="w-7 h-7 rounded-lg bg-indigo-500/20 text-indigo-600 dark:text-indigo-300 border border-indigo-500/30 font-extrabold flex items-center justify-center text-xs">
                {currentUser.fullName ? currentUser.fullName.charAt(0) : 'U'}
              </div>
              <div className="hidden sm:block text-left leading-tight">
                <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block truncate max-w-[120px]">
                  {currentUser.fullName || currentUser.email}
                </span>
                <span className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold block">
                  {currentUser.role}
                </span>
              </div>
            </button>

            {/* Botón Cerrar Sesión */}
            <button
              onClick={handleLogout}
              className="px-3 py-1.5 rounded-xl text-xs font-bold bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 dark:text-rose-300 border border-rose-500/30 transition-all flex items-center gap-1.5"
            >
              <span>Cerrar</span>
              <span className="hidden sm:inline">Sesión</span>
            </button>
          </div>
        </div>

        {/* Sub-navegación si el módulo activo es Cliente */}
        {activeModule === 'client' && isClient && (
          <div className="max-w-7xl mx-auto mt-2 pt-2 border-t border-slate-200 dark:border-slate-800/80 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setClientSubView('checkout')}
                className={`px-3 py-1 rounded-md font-bold transition-all flex items-center gap-1.5 ${
                  clientSubView === 'checkout'
                    ? 'bg-amber-500/20 text-amber-700 dark:text-amber-400 border border-amber-500/40'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>🛒</span>
                <span>Menú & Checkout ($ USD)</span>
              </button>

              <button
                onClick={() => setClientSubView('tracking')}
                className={`px-3 py-1 rounded-md font-bold transition-all flex items-center gap-1.5 ${
                  clientSubView === 'tracking'
                    ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 border border-emerald-500/40'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <span>📍</span>
                <span>Seguimiento GPS & PIN</span>
              </button>
            </div>

            <span className="hidden sm:inline text-[11px] text-slate-500 dark:text-slate-400">
              Ubicación: <strong className="text-slate-800 dark:text-slate-200">Tulcán, Carchi</strong> • Moneda: <strong className="text-emerald-600 dark:text-emerald-400">$ USD</strong>
            </span>
          </div>
        )}

        {/* Sub-navegación si el módulo activo es Comercio o Repartidor */}
        {(activeModule === 'merchant' || activeModule === 'driver') && (
          <div className="max-w-7xl mx-auto mt-2 pt-2 border-t border-slate-200 dark:border-slate-800/80 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                {activeModule === 'merchant' ? '🍳 Panel de Cocina KDS y Menú' : '🛵 Operaciones de Ruta y Reparto'}
              </span>
            </div>

            <button
              id="subnav-bank-settings-btn"
              onClick={() => setShowBankModal(true)}
              className="flex items-center gap-1.5 px-3 py-1 rounded-md font-bold bg-amber-500/15 hover:bg-amber-500/25 text-amber-700 dark:text-amber-300 border border-amber-500/40 transition-all text-xs shadow-sm cursor-pointer"
            >
              <span>💳</span>
              <span>Ajustes / Cuenta de Cobro & QR</span>
            </button>
          </div>
        )}
      </nav>

      {/* ── 2. CONTENIDO PRINCIPAL PROTEGIDO CON GUARDIA RBAC ─────────────────── */}
      <main className="flex-1">
        <ProtectedRoute
          user={currentUser}
          allowedRoles={moduleRolesMap[activeModule] || ['ROLE_ADMIN']}
          onUnauthorized={handleUnauthorized}
        >
          {activeModule === 'client' && (
            clientSubView === 'checkout' ? (
              <RestaurantCheckout onOrderConfirmed={handleOrderConfirmed} />
            ) : (
              <TrackingPage initialOrder={createdOrder} onBackToMenu={() => setClientSubView('checkout')} />
            )
          )}

          {activeModule === 'driver' && (
            /* Vista adaptada estilo App Nativa para Smartphone */
            <div className="max-w-md mx-auto shadow-2xl min-h-screen bg-slate-100 dark:bg-slate-950 transition-colors duration-200">
              <ActiveTrip activeOrder={createdOrder} />
            </div>
          )}

          {activeModule === 'merchant' && <MerchantKitchen />}

          {activeModule === 'admin' && <MerchantApprovals />}
        </ProtectedRoute>
      </main>

      {/* ── 3. NAVEGACIÓN INFERIOR MOBILE ESTILO APP NATIVA (RBAC RESTRINGIDO) ─── */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-50 bg-white/95 dark:bg-slate-950/95 backdrop-blur-lg border-t border-slate-200 dark:border-slate-800 px-2 py-1.5 flex items-center justify-around shadow-2xl">
        
        {/* Tab 1: Cliente */}
        {isClient && (
          <button
            onClick={() => setActiveModule('client')}
            className={`flex flex-col items-center justify-center p-1.5 rounded-xl min-w-[64px] min-h-[48px] transition-all ${
              activeModule === 'client'
                ? 'text-amber-500 font-extrabold bg-amber-500/10'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="text-lg">🛒</span>
            <span className="text-[10px]">Cliente</span>
          </button>
        )}

        {/* Tab 2: Repartidor */}
        {isDriver && (
          <button
            onClick={() => setActiveModule('driver')}
            className={`flex flex-col items-center justify-center p-1.5 rounded-xl min-w-[64px] min-h-[48px] transition-all ${
              activeModule === 'driver'
                ? 'text-emerald-500 font-extrabold bg-emerald-500/10'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="text-lg">🛵</span>
            <span className="text-[10px]">Repartidor</span>
          </button>
        )}

        {/* Tab 3: Cocina */}
        {isMerchant && (
          <button
            onClick={() => setActiveModule('merchant')}
            className={`flex flex-col items-center justify-center p-1.5 rounded-xl min-w-[64px] min-h-[48px] transition-all ${
              activeModule === 'merchant'
                ? 'text-rose-500 font-extrabold bg-rose-500/10'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="text-lg">🍳</span>
            <span className="text-[10px]">Cocina</span>
          </button>
        )}

        {/* Tab 4: Administrador */}
        {isAdmin && (
          <button
            onClick={() => setActiveModule('admin')}
            className={`flex flex-col items-center justify-center p-1.5 rounded-xl min-w-[64px] min-h-[48px] transition-all ${
              activeModule === 'admin'
                ? 'text-indigo-500 font-extrabold bg-indigo-500/10'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <span className="text-lg">🛡️</span>
            <span className="text-[10px]">Admin</span>
          </button>
        )}
      </div>

      {/* ── 4. MODAL INSPECTOR DE TOKEN JWT ─────────────────────────────────── */}
      {showTokenModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-3xl max-w-lg w-full p-6 shadow-2xl text-slate-900 dark:text-slate-100 animate-fadeIn transition-colors">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2.5">
                <span className="text-2xl">🔐</span>
                <div>
                  <h3 className="font-extrabold text-base text-slate-900 dark:text-white">
                    Sesión JWT del Usuario
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Claims criptográficos decodificados en el Frontend
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowTokenModal(false)}
                className="w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 flex items-center justify-center text-slate-500 dark:text-slate-300 font-bold"
              >
                ✕
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs">
              <div className="bg-slate-50 dark:bg-slate-800/80 p-3 rounded-xl border border-slate-200 dark:border-slate-700/60">
                <span className="text-slate-500 dark:text-slate-400 block mb-0.5">Nombre Completo:</span>
                <span className="font-bold text-slate-900 dark:text-white text-sm">{currentUser.fullName}</span>
              </div>

              <div className="bg-slate-50 dark:bg-slate-800/80 p-3 rounded-xl border border-slate-200 dark:border-slate-700/60 flex justify-between items-center">
                <div>
                  <span className="text-slate-500 dark:text-slate-400 block mb-0.5">Correo Electrónico (Subject):</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">{currentUser.email}</span>
                </div>
                <div className="text-right">
                  <span className="text-slate-500 dark:text-slate-400 block mb-0.5">Rol Decodificado:</span>
                  <span className="px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 font-extrabold border border-emerald-500/30">
                    {currentUser.role}
                  </span>
                </div>
              </div>

              <div className="bg-slate-50 dark:bg-slate-800/80 p-3 rounded-xl border border-slate-200 dark:border-slate-700/60">
                <span className="text-slate-500 dark:text-slate-400 block mb-1">Payload Decodificado (Claims JSON):</span>
                <pre className="font-mono text-[10px] text-indigo-600 dark:text-indigo-300 bg-white dark:bg-slate-950 p-2.5 rounded-lg border border-slate-200 dark:border-slate-800 max-h-24 overflow-y-auto">
                  {JSON.stringify(decodeJwt(getAuthToken()), null, 2)}
                </pre>
              </div>

              <div className="bg-slate-50 dark:bg-slate-800/80 p-3 rounded-xl border border-slate-200 dark:border-slate-700/60">
                <span className="text-slate-500 dark:text-slate-400 block mb-1">Token JWT Bearer Almacenado:</span>
                <div className="font-mono text-[10px] text-amber-600 dark:text-amber-300 break-all bg-white dark:bg-slate-950 p-2.5 rounded-lg border border-slate-200 dark:border-slate-800 max-h-20 overflow-y-auto">
                  {getAuthToken()}
                </div>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setShowTokenModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-bold text-xs"
              >
                Cerrar
              </button>
              <button
                onClick={handleLogout}
                className="px-4 py-2 rounded-xl bg-rose-500 hover:bg-rose-400 text-white font-bold text-xs"
              >
                Cerrar Sesión Activa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── 5. MODAL AJUSTES DE CUENTA BANCARIA Y COBRO QR ─────────────────── */}
      {showBankModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto animate-fadeIn">
          <div className="max-w-2xl w-full my-auto">
            <BankSettingsSection
              userType={activeModule === 'driver' ? 'driver' : 'merchant'}
              onClose={() => setShowBankModal(false)}
              onSaved={() => {
                setTimeout(() => setShowBankModal(false), 1200);
              }}
            />
          </div>
        </div>
      )}

    </div>
  );
}
