// helpers genéricos
export const qs  = (sel, el = document) => el.querySelector(sel);
export const qsa = (sel, el = document) => el.querySelectorAll(sel);
export const create = (t, cls = "") => { const e = document.createElement(t); if (cls) e.className = cls; return e; };
export const nf = v => (v === "" ? null : +v);

export const TOKEN_KEY = "gym_token";
export const API_BASE  = import.meta?.env?.VITE_API_BASE
                      || "https://appgym-production-64ac.up.railway.app";

export const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* ---------- Loader global ---------- */
export function showLoader(show = true){
  let ov = qs("#loader-overlay");
  if (!ov){
    ov = create("div"); ov.id = "loader-overlay"; ov.className = "hidden";
    ov.innerHTML = '<div class="spinner"></div>';
    document.body.appendChild(ov);
  }
  ov.classList.toggle("hidden", !show);
}
