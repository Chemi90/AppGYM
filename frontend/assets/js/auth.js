/* -------------------------------------------------------------------------
   AUTH · login / registro
   ------------------------------------------------------------------------- */
import { API_BASE, TOKEN_KEY, qs, dbg } from "./utils.js";

dbg('AUTH', 'init');

/* --- elementos DOM --- */
const form     = qs('#auth-form');
const confirm  = qs('#confirm');
const toggle   = qs('#toggle-link');
const title    = qs('#form-title');
const submitBt = qs('#submit-btn');

let mode = 'login';          // estado inicial (login)

/* --- alterna login / register --------------------------------------- */
toggle.addEventListener('click', ev => {
  ev.preventDefault();
  mode = mode === 'login' ? 'register' : 'login';

  /* añade/quita atributo hidden */
  confirm.toggleAttribute('hidden', mode === 'login');

  /* textos */
  title.textContent    = mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta';
  submitBt.textContent = mode === 'login' ? 'Entrar'         : 'Registrar';
  toggle.textContent   = mode === 'login'
    ? '¿No tienes cuenta? Regístrate'
    : '¿Ya tienes cuenta? Inicia sesión';
});

/* --- envío ----------------------------------------------------------- */
form.onsubmit = async ev => {
  ev.preventDefault();
  submitBt.disabled = true;
  submitBt.textContent = '⏳ Enviando…';

  try {
    const body = { email: form.email.value, password: form.password.value };
    if (mode === 'register') body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method : 'POST',
      headers: { 'Content-Type':'application/json' },
      body   : JSON.stringify(body)
    });

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
