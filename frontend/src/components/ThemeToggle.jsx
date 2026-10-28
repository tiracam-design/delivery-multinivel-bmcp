import React, { useState, useEffect } from 'react';
import { getInitialTheme, toggleTheme, applyTheme } from '../utils/theme.js';

export default function ThemeToggle({ className = '' }) {
  const [theme, setTheme] = useState(getInitialTheme());

  useEffect(() => {
    // Aplicar tema inicial
    applyTheme(theme);

    const handleThemeChange = (e) => {
      setTheme(e.detail);
    };

    window.addEventListener('theme-changed', handleThemeChange);
    return () => window.removeEventListener('theme-changed', handleThemeChange);
  }, []);

  const handleToggle = () => {
    const next = toggleTheme();
    setTheme(next);
  };

  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={handleToggle}
      className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold transition-all shadow-md backdrop-blur-md ${
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
