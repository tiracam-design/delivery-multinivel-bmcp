// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Selector de Cuentas Google OAuth con Decodificación de Roles Dinámicos
// ═══════════════════════════════════════════════════════════════════════

import React, { useState } from 'react';
import { loginWithGoogle } from '../../utils/auth.js';

export default function GoogleAuthModal({ isOpen, onClose, onLoginSuccess }) {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  if (!isOpen) return null;

  const accounts = [
    {
      name: 'Valeria Narváez Guerrón',
      email: 'valeria.narvaez@tulcan.ec',
      role: 'CLIENT',
      roleLabel: 'Cliente',
      avatarBg: 'bg-amber-500',
      initials: 'VN'
    },
    {
      name: 'Carlos Mendoza Revelo',
      email: 'carlos.mendoza@tulcan.ec',
      role: 'DRIVER',
      roleLabel: 'Repartidor',
      avatarBg: 'bg-emerald-500',
      initials: 'CM'
    },
    {
      name: 'Patricio Benavides Cárdenas',
      email: 'patricio.benavides@lenadortulcan.ec',
      role: 'MERCHANT',
      roleLabel: 'Restaurante / Cocina',
      avatarBg: 'bg-rose-500',
      initials: 'PB'
    },
    {
      name: 'Administración GAD Municipal Tulcán',
      email: 'admin@deliverytulcan.ec',
      role: 'ADMIN',
      roleLabel: 'Administrador GAD',
      avatarBg: 'bg-indigo-600',
      initials: 'AD'
    }
  ];

  const handleSelectAccount = async (account) => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const user = await loginWithGoogle(account.role, account.email);
      if (onLoginSuccess) {
        onLoginSuccess(user);
      }
      onClose();
    } catch (err) {
      setErrorMessage(err.message || 'Error al autenticar con cuenta de Google.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/75 backdrop-blur-md flex items-center justify-center p-3 sm:p-4">
      <div className="relative w-full max-w-md bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 p-6 sm:p-7 animate-fadeIn">
        
        {/* Botón Cerrar */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 flex items-center justify-center text-sm font-bold transition-all"
        >
          ✕
        </button>

        {/* Encabezado estilo Google */}
        <div className="text-center mb-5">
          <div className="inline-flex items-center justify-center w-11 h-11 rounded-full bg-slate-50 dark:bg-slate-800 shadow-sm border border-slate-200 dark:border-slate-700 mb-2">
            <svg className="w-5 h-5" viewBox="0 0 24 24">
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
          </div>
          <h3 className="text-lg font-bold text-slate-900 dark:text-white">
            Iniciar sesión con Google
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Selecciona una cuenta para ingresar a Delivery Tulcán
          </p>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-300 text-xs">
            {errorMessage}
          </div>
        )}

        {/* Lista de Cuentas */}
        <div className="divide-y divide-slate-100 dark:divide-slate-800 border-y border-slate-100 dark:border-slate-800 mb-4">
          {accounts.map((acc) => (
            <button
              key={acc.email}
              type="button"
              disabled={isLoading}
              onClick={() => handleSelectAccount(acc)}
              className="w-full py-3 px-2 flex items-center gap-3 text-left hover:bg-slate-50 dark:hover:bg-slate-800/60 rounded-xl transition-all group disabled:opacity-50"
            >
              <div
                className={`w-9 h-9 rounded-full ${acc.avatarBg} text-white font-bold text-xs flex items-center justify-center shadow-sm flex-shrink-0 group-hover:scale-105 transition-transform`}
              >
                {acc.initials}
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-xs font-bold text-slate-900 dark:text-white block truncate">
                  {acc.name}
                </span>
                <span className="text-[11px] text-slate-500 dark:text-slate-400 block truncate">
                  {acc.email}
                </span>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                {acc.roleLabel}
              </span>
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center gap-2 py-2 text-xs text-slate-500 dark:text-slate-400 font-semibold">
            <div className="w-4 h-4 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
            <span>Verificando token JWT en Spring Boot...</span>
          </div>
        ) : (
          <p className="text-[11px] text-slate-400 text-center">
            Para continuar, Google compartirá tu nombre, correo y avatar con Delivery BMCP.
          </p>
        )}

      </div>
    </div>
  );
}
