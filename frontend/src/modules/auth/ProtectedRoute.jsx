// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Componente de Guardia de Rutas y Control RBAC (ProtectedRoute)
// ═══════════════════════════════════════════════════════════════════════

import React, { useEffect, useState } from 'react';
import { getAuthToken, hasAnyRole } from '../../utils/auth.js';

/**
 * Protege vistas y módulos contra accesos de usuarios no registrados o con roles no autorizados.
 * Si un usuario intenta acceder a un módulo ajeno, lo reorienta automáticamente al login.
 * 
 * @param {Object} props
 * @param {Object|null} props.user - Usuario autenticado
 * @param {string[]} props.allowedRoles - Lista de roles permitidos para este módulo
 * @param {Function} props.onUnauthorized - Callback para reorientar al login
 * @param {React.ReactNode} props.children - Componente hijo protegido
 */
export default function ProtectedRoute({ user, allowedRoles = [], onUnauthorized, children }) {
  const [isRedirecting, setIsRedirecting] = useState(false);
  const token = getAuthToken();

  const isUserAuthenticated = Boolean(user && token);
  const isAuthorized = isUserAuthenticated && hasAnyRole(user, allowedRoles);

  useEffect(() => {
    if (!isUserAuthenticated) {
      console.warn('⛔ [RBAC] Intento de acceso sin autenticación. Reorientando al login...');
      if (onUnauthorized) {
        onUnauthorized('unauthenticated');
      }
    } else if (!isAuthorized) {
      console.warn(
        `⛔ [RBAC] Acceso no autorizado: El usuario con rol ${user?.role} intentó ingresar a un módulo restringido. Reorientando al login...`
      );
      setIsRedirecting(true);
      const timer = setTimeout(() => {
        if (onUnauthorized) {
          onUnauthorized('forbidden');
        }
      }, 1200);

      return () => clearTimeout(timer);
    }
  }, [isUserAuthenticated, isAuthorized, user, onUnauthorized]);

  // Si no está autenticado, no renderizar nada mientras se procesa la reorientación
  if (!isUserAuthenticated) {
    return null;
  }

  // Si el rol no está autorizado para este módulo, mostrar pantalla de reorientación
  if (!isAuthorized || isRedirecting) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center p-4">
        <div className="max-w-md w-full p-6 sm:p-8 bg-slate-900/90 dark:bg-slate-900/90 bg-white/95 backdrop-blur-xl border border-rose-500/40 rounded-3xl shadow-2xl text-center animate-shake">
          <div className="w-16 h-16 rounded-2xl bg-rose-500/20 text-rose-500 flex items-center justify-center text-3xl mx-auto mb-4 border border-rose-500/30">
            ⛔
          </div>
          <h2 className="text-xl font-black text-rose-500 mb-2">
            Acceso No Autorizado (HTTP 403)
          </h2>
          <p className="text-sm text-slate-300 dark:text-slate-300 text-slate-600 mb-4 leading-relaxed">
            Tu rol <strong className="text-amber-400 font-bold">{user.role}</strong> no tiene permisos para operar en este módulo.
          </p>
          <div className="flex items-center justify-center gap-2 text-xs font-semibold text-slate-400">
            <div className="w-3.5 h-3.5 border-2 border-rose-500 border-t-transparent rounded-full animate-spin"></div>
            <span>Reorientando automáticamente al inicio de sesión...</span>
          </div>
        </div>
      </div>
    );
  }

  // Usuario autenticado y con rol autorizado
  return <>{children}</>;
}
