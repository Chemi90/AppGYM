/* ───────────────────────────────────────────────────────────────────────────
   Utilidades globales
   - selectores rápidos
   - loader overlay
   - función dbg()   ← NUEVA
   - registro Service-Worker con recarga auto
   ───────────────────────────────────────────────────────────────────────── */

export const qs  = (sel, el = document) => el.querySelector(sel);
export const qsa = (sel, el = document) => el.querySelectorAll(sel);
export const create = (tag, cls = '') => {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  return e;
};

/* ---------- num / null ---------- */
export const nf = v => (v === '' ? null : +v);

/* ---------- tiny logger ---------- */
export function dbg (ns, ...args) {
  /*  Desactiva todas las trazas poniendo DEBUG = false                  */
  const DEBUG = true;
  /*  Filtra por namespace si quieres – p.ej. if(ns!=='TIMER') return;   */
  if (!DEBUG) return;
  console.log(`%c[${ns}]`, 'color:#7c3aed;font-weight:700', ...args);
}

/* ---------- API / Autenticación ---------- */
export const TOKEN_KEY = 'gym_token';
export const API_BASE  =
  import.meta?.env?.VITE_API_BASE || 'https://appgym-production-64ac.up.railway.app';

export const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* ---------- Loader Overlay ---------- */
export function showLoader (on = true) { qs('#loader-overlay')?.classList.toggle('hidden', !on); }
export const hideLoader = () => showLoader(false);

/* =================  SERVICE-WORKER ================= */
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').then(reg => {
    dbg('SW', 'Registrado', reg);

    /* SW nuevo ya descargado */
    if (reg.waiting) activateSW(reg);

    /* SW nuevo mientras usamos la app */
    reg.addEventListener('updatefound', () => {
      reg.installing.addEventListener('statechange', () => {
        if (reg.waiting) activateSW(reg);
      });
    });
  });

  /* Recarga cuando hay nuevo controlador */
  let refreshing = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (refreshing) return;
    refreshing = true;
    location.reload();
  });
}

function activateSW (reg) {
  reg.waiting.postMessage({ type: 'SKIP_WAITING' });
}
