// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Gestión de Tema Global: Modo Oscuro 🌙 / Modo Claro ☀️
// ═══════════════════════════════════════════════════════════════════════

const THEME_KEY = 'delivery_theme';

/**
 * Obtiene el tema actual configurado en localStorage o por defecto 'dark'
 * para conservar la estética glassmorphism con mapa oscuro.
 * @returns {'dark'|'light'}
 */
export function getInitialTheme() {
  if (typeof window === 'undefined') return 'dark';
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'dark' || saved === 'light') {
    return saved;
  }
  return 'dark'; // Predeterminado: Modo Oscuro con mapa nocturno
}

/**
 * Aplica el tema agregando o removiendo la clase 'dark' en <html>
 * y persiste la preferencia en localStorage.
 * @param {'dark'|'light'} theme
 */
export function applyTheme(theme) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  if (theme === 'dark') {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
  localStorage.setItem(THEME_KEY, theme);
  window.dispatchEvent(new CustomEvent('theme-changed', { detail: theme }));
}

/**
 * Alterna entre modo oscuro y claro.
 * @returns {'dark'|'light'} nuevo tema
 */
export function toggleTheme() {
  const current = getInitialTheme();
  const next = current === 'dark' ? 'light' : 'dark';
  applyTheme(next);
  return next;
}
