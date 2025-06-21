/* =========================================================================
   AUTH · Login / Registro  (con dbg logs)
   ========================================================================= */
import { API_BASE, TOKEN_KEY, qs, dbg } from "./utils.js";

/* --- DOM refs --- */
const form     = qs('#auth-form');
const confirm  = qs('#confirm');
const toggle   = qs('#toggle-link');
const title    = qs('#form-title');
const submitBt = qs('#submit-btn');

let mode = 'login';                       // estado inicial
confirm.classList.add('hidden');          // oculto de partida

dbg('AUTH', 'init');

/* --- toggle login/register ------------------------------------------ */
toggle.addEventListener('click', ev => {
  ev.preventDefault();
  mode = mode === 'login' ? 'register' : 'login';
  dbg('AUTH', 'toggle', mode);

  confirm.classList.toggle('hidden', mode === 'login');
  title.textContent    = mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta';
  submitBt.textContent = mode === 'login' ? 'Entrar'         : 'Registrar';
  toggle.textContent   = mode === 'login'
      ? '¿No tienes cuenta? Regístrate'
      : '¿Ya tienes cuenta? Inicia sesión';
});

/* --- submit ---------------------------------------------------------- */
form.onsubmit = async ev => {
  ev.preventDefault();
  dbg('AUTH', 'submit', mode);

  /* UI lock */
  submitBt.disabled   = true;
  submitBt.textContent = '⏳ Enviando…';

  try {
    const body = { email: form.email.value, password: form.password.value };
    if (mode === 'register') body.confirm = form.confirm.value;

    const url = `${API_BASE}/api/auth/${mode}`;
    dbg('AUTH', 'FETCH', url, body);

    const res = await fetch(url, {
      method : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body   : JSON.stringify(body)
    });

    dbg('AUTH', 'response', res.status);
    if (!res.ok) { alert(await res.text()); return; }

    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = 'dashboard.html';

  } catch (err) {
    console.error(err);
    alert('Error de red');
  } finally {
    submitBt.disabled  = false;
    submitBt.textContent = mode === 'login' ? 'Entrar' : 'Registrar';
  }
};
