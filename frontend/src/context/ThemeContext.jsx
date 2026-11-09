// ══════════════════════════════════════════════════════════════════════════
// ThemeContext — Delivery Multinivel BMCP · Tulcán, Ecuador
// Contexto global de tema: provee isDark + toggle a toda la app.
// Uso: const { isDark, toggle } = useTheme()
// ══════════════════════════════════════════════════════════════════════════

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getInitialTheme, applyTheme } from '../utils/theme.js';

const ThemeContext = createContext({ isDark: true, toggle: () => {} });

export function ThemeProvider({ children }) {
  const [isDark, setIsDark] = useState(() => getInitialTheme() === 'dark');

  // Aplica el tema al <html> y persiste en localStorage
  const applyAndSync = useCallback((dark) => {
    applyTheme(dark ? 'dark' : 'light');
    setIsDark(dark);
  }, []);

  // Inicializar al montar
  useEffect(() => {
    applyAndSync(getInitialTheme() === 'dark');
  }, []);

  // Escuchar cambios externos (ThemeToggle u otras pestañas)
  useEffect(() => {
    const handleExternal = (e) => setIsDark(e.detail === 'dark');
    window.addEventListener('theme-changed', handleExternal);
    return () => window.removeEventListener('theme-changed', handleExternal);
  }, []);

  const toggle = useCallback(() => {
    applyAndSync(!isDark);
  }, [isDark, applyAndSync]);

  return (
    <ThemeContext.Provider value={{ isDark, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}

