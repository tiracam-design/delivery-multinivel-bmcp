// ══════════════════════════════════════════════════════════════════════════
// ThemeToggle — Delivery Multinivel BMCP · Tulcán, Ecuador
// Botón de conmutación global de tema. Usa ThemeContext centralizado.
// ══════════════════════════════════════════════════════════════════════════

import React from 'react';
import { useTheme } from '../context/ThemeContext.jsx';

export default function ThemeToggle({ className = '' }) {
  const { isDark, toggle } = useTheme();

  return (
    <button
      type="button"
      onClick={toggle}
      className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition-all duration-200 shadow-md backdrop-blur-md ${
        isDark
          ? 'bg-slate-800/80 hover:bg-slate-800 text-amber-300 border border-slate-700/80 hover:border-amber-400/50'
          : 'bg-white/80 hover:bg-white text-slate-700 border border-slate-200/80 hover:border-indigo-400/50 shadow-slate-200'
      } ${className}`}
      title={isDark ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro'}
      aria-label="Conmutador de tema"
    >
      <span className="text-base leading-none">
        {isDark ? '🌙' : '☀️'}
      </span>
      <span className="hidden sm:inline">
        {isDark ? 'Modo Oscuro' : 'Modo Claro'}
      </span>
    </button>
  );
}
