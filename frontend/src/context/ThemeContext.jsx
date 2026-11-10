// ══════════════════════════════════════════════════════════════════════════
// ThemeContext — Delivery Multinivel BMCP · Tulcán, Ecuador
//
// Provee { isDark, toggle } a toda la app vía React Context.
// El tema inicial lo lee desde el DOM (<html class="dark">) que fue
// configurado por el script anti-FOUC en index.html, evitando FOUC.
// ══════════════════════════════════════════════════════════════════════════

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { applyTheme } from '../utils/theme.js';

const ThemeContext = createContext({ isDark: true, toggle: () => {} });

export function ThemeProvider({ children }) {
  // Lee el estado real del DOM (lo estableció el script bloqueante en index.html)
  // — esto garantiza que React y el DOM estén sincronizados desde el primer render.
  const [isDark, setIsDark] = useState(() => {
    if (typeof document !== 'undefined') {
      return document.documentElement.classList.contains('dark');
    }
    return true; // SSR fallback
  });

  // Escucha cambios de tema provenientes de otras pestañas o de toggleTheme()
  useEffect(() => {
    const handleExternal = (e) => {
      setIsDark(e.detail === 'dark');
    };
    window.addEventListener('theme-changed', handleExternal);
    return () => window.removeEventListener('theme-changed', handleExternal);
  }, []);

  const toggle = useCallback(() => {
    const next = !isDark;
    applyTheme(next ? 'dark' : 'light');
    setIsDark(next);
  }, [isDark]);

  return (
    <ThemeContext.Provider value={{ isDark, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
