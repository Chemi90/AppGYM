/* =========================================================================
   AUTH · Controla login y registro
   ========================================================================= */
import { API_BASE, TOKEN_KEY, qs } from "./utils.js";

/* --- elementos --- */
const form     = qs("#auth-form");
const confirm  = qs("#confirm");
const toggle   = qs("#toggle-link");
const title    = qs("#form-title");
const submitBt = qs("#submit-btn");

let mode = "login";                // estado inicial

/* --- asegúrate de que el campo confirm. está oculto de entrada --- */
confirm.classList.add("hidden");

/* --- alterna login / register -------------------------------------- */
toggle.addEventListener("click", ev => {
  ev.preventDefault();
  mode = mode === "login" ? "register" : "login";
  confirm.classList.toggle("hidden", mode === "login");
  title.textContent       = mode === "login" ? "Iniciar sesión" : "Crear cuenta";
  submitBt.textContent    = mode === "login" ? "Entrar"         : "Registrar";
  toggle.textContent      = mode === "login"
      ? "¿No tienes cuenta? Regístrate"
      : "¿Ya tienes cuenta? Inicia sesión";
});

/* --- envío ---------------------------------------------------------- */
form.onsubmit = async ev => {
  ev.preventDefault();

  /* bloqueo UI */
  submitBt.disabled = true;
  submitBt.textContent = "⏳ Enviando…";

  try{
    const body = { email: form.email.value, password: form.password.value };
    if (mode === "register") body.confirm = form.confirm.value;

    const res  = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method : "POST",
      headers: { "Content-Type": "application/json" },
      body   : JSON.stringify(body)
    });

    if (!res.ok){
      alert(await res.text());
      return;
    }
    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = "dashboard.html";
  }catch(err){
    console.error(err);
    alert("Error de red");
  }finally{
    submitBt.disabled  = false;
    submitBt.textContent = mode === "login" ? "Entrar" : "Registrar";
  }
};
