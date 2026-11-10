/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // ── Escala slate extendida ──────────────────────────────────────────
        slate: {
          850: '#151e2e',
        },
        // ── Tokens semánticos del sistema BMCP ─────────────────────────────
        // Superficie principal (fondo de página)
        surface: {
          DEFAULT:  '#f8fafc', // slate-50  — modo claro
          dark:     '#020617', // slate-950 — modo oscuro
        },
        // Superficie elevada (cards, paneles)
        panel: {
          DEFAULT:  '#ffffff', // white     — modo claro
          dark:     '#0f172a', // slate-900 — modo oscuro
        },
        // Superficie secundaria (inputs, badges, secciones internas)
        muted: {
          DEFAULT:  '#f1f5f9', // slate-100 — modo claro
          dark:     '#1e293b', // slate-800 — modo oscuro
        },
        // Borde estándar
        border: {
          DEFAULT:  '#e2e8f0', // slate-200 — modo claro
          dark:     '#334155', // slate-700 — modo oscuro
        },
        // Texto principal
        ink: {
          DEFAULT:  '#0f172a', // slate-900 — modo claro
          dark:     '#f1f5f9', // slate-100 — modo oscuro
        },
        // Texto secundario / muted
        subtle: {
          DEFAULT:  '#64748b', // slate-500 — modo claro
          dark:     '#94a3b8', // slate-400 — modo oscuro
        },
        // ── Acento primario: Ámbar (dominante) ─────────────────────────────
        brand: {
          50:   '#fffbeb',
          100:  '#fef3c7',
          200:  '#fde68a',
          300:  '#fcd34d',
          400:  '#fbbf24',
          500:  '#f59e0b', // base
          600:  '#d97706',
          700:  '#b45309',
          800:  '#92400e',
          900:  '#78350f',
          950:  '#451a03',
        },
        // ── Acento secundario: Esmeralda (contraste) ───────────────────────
        accent: {
          50:   '#ecfdf5',
          100:  '#d1fae5',
          200:  '#a7f3d0',
          300:  '#6ee7b7',
          400:  '#34d399',
          500:  '#10b981', // base
          600:  '#059669',
          700:  '#047857',
          800:  '#065f46',
          900:  '#064e3b',
          950:  '#022c22',
        },
      },
      // ── Transición de tema ──────────────────────────────────────────────
      transitionProperty: {
        theme: 'color, background-color, border-color, fill, stroke',
      },
      transitionDuration: {
        theme: '200ms',
      },
    },
  },
  plugins: [],
}
