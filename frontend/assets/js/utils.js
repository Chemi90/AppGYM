/* ────────────────────────────────────────────────────────────────────
   Utilidades globales  |  Todas las vistas importan de aquí
   Incluye registro del Service-Worker con recarga automática
   ──────────────────────────────────────────────────────────────────── */

/* ---------------- SELECTORES RÁPIDOS ---------------- */
export const qs  = (sel, el = document) => el.querySelector(sel);
export const qsa = (sel, el = document) => el.querySelectorAll(sel);
export const create = (tag, cls = '') => {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  return e;
};

/* ---------------- CONVERSIÓN num/null -------------- */
export const nf = v => (v === '' ? null : +v);

/* ---------------- CONSTANTES API ------------------- */
export const TOKEN_KEY = 'gym_token';
export const API_BASE  =
  import.meta?.env?.VITE_API_BASE || 'https://appgym-production-64ac.up.railway.app';

export const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* ---------------- LOADER GLOBAL -------------------- */
export function showLoader () { qs('#loader-overlay')?.classList.remove('hidden'); }
export function hideLoader () { qs('#loader-overlay')?.classList.add('hidden'); }

/* =================  SERVICE-WORKER  ================= */
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').then(reg => {
    /* — SW NUEVO YA DESCARGADO — */
    if (reg.waiting) activateSW(reg);

    /* — SW NUEVO MIENTRAS SE USA LA APP — */
    reg.addEventListener('updatefound', () => {
      const nw = reg.installing;
      nw.addEventListener('statechange', () => {
        if (reg.waiting) activateSW(reg);
      });
    });
  });

  /* Recarga la página cuando el nuevo SW toma el control */
  let refreshing = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (refreshing) return;
    refreshing = true;
    location.reload();
  });
}

function activateSW (reg) {
  /* Pedimos al SW que pase de waiting → active YA */
  reg.waiting.postMessage({ type: 'SKIP_WAITING' });
}
