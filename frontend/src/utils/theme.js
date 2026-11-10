// ═══════════════════════════════════════════════════════════════════════
// Delivery Multinivel BMCP — Tulcán, Ecuador
// Gestión de Tema Global: Modo Oscuro 🌙 / Modo Claro ☀️
//
// Nota de arquitectura:
// El script bloqueante en index.html aplica la clase 'dark' al <html>
// ANTES del primer paint (anti-FOUC). Esta utilidad sincroniza el estado
// de React con esa decisión y gestiona los cambios posteriores.
// ═══════════════════════════════════════════════════════════════════════

const THEME_KEY = 'delivery_theme';

/**
 * Lee el tema preferido desde localStorage.
 * Fallback: 'dark' (coincide con la lógica del script anti-FOUC en index.html).
 * @returns {'dark'|'light'}
 */
export function getInitialTheme() {
  if (typeof window === 'undefined') return 'dark';
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'dark' || saved === 'light') return saved;
  return 'dark';
}

/**
 * Aplica el tema: agrega/remueve la clase 'dark' en <html>,
 * persiste en localStorage y notifica a otros listeners vía CustomEvent.
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

  // Notifica a ThemeContext y a otras pestañas
  window.dispatchEvent(new CustomEvent('theme-changed', { detail: theme }));
}

/**
 * Alterna entre modo oscuro y claro.
 * @returns {'dark'|'light'} nuevo tema aplicado
 */
export function toggleTheme() {
  const current = getInitialTheme();
  const next = current === 'dark' ? 'light' : 'dark';
  applyTheme(next);
  return next;
}
