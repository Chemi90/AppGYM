/* ────────────────────────────────────────────────────────────────
   Utilidades globales  +  Service-Worker  +  dbg()
   ──────────────────────────────────────────────────────────────── */
export const qs  = (sel, el = document) => el.querySelector(sel);
export const qsa = (sel, el = document) => el.querySelectorAll(sel);
export const create = (t, cls = '') => { const e = document.createElement(t); if (cls) e.className = cls; return e; };

/* logging helper */
export const dbg = (tag, ...args) => console.log(`%c[${tag}]`, 'color:#8b5cf6;font-weight:700', ...args);

/* num | null */
export const nf = v => (v === '' ? null : +v);

/* API & token */
export const TOKEN_KEY  = 'gym_token';
export const API_BASE   = import.meta?.env?.VITE_API_BASE || 'https://appgym-production-64ac.up.railway.app';
export const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* loader overlay */
export function showLoader () { qs('#loader-overlay')?.classList.remove('hidden'); }
export function hideLoader () { qs('#loader-overlay')?.classList.add('hidden'); }

/* ---------- Service-Worker auto-refresh ---------- */
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').then(reg => {
    dbg('SW', 'Registrado', reg);

    if (reg.waiting) activate(reg);
    reg.addEventListener('updatefound', () => {
      dbg('SW', 'updatefound');
      reg.installing.addEventListener('statechange', () => {
        dbg('SW', 'statechange →', reg.installing.state);
        if (reg.waiting) activate(reg);
      });
    });
  });

  navigator.serviceWorker.addEventListener('controllerchange', () => {
    dbg('SW', 'controllerchange → recarga');
    location.reload();
  });
}

function activate (reg) {
  dbg('SW', 'Activando nuevo SW…');
  reg.waiting.postMessage({ type: 'SKIP_WAITING' });
}
