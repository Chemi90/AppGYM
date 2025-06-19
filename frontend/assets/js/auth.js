import { API_BASE, TOKEN_KEY, qs } from "../utils.js";

const form    = qs("#auth-form");
const confirm = qs("#confirm");
const toggle  = qs("#toggle-link");
let   mode    = "login";

toggle.onclick = e => { e.preventDefault(); swap(); };
function swap(){
  mode = mode === "login" ? "register" : "login";
  confirm.classList.toggle("hidden", mode === "login");
  qs("#form-title").textContent = mode === "login" ? "Iniciar sesión" : "Crear cuenta";
  qs("#submit-btn").textContent = mode === "login" ? "Entrar" : "Registrar";
}

form.onsubmit = async e => {
  e.preventDefault();
  const body = { email:form.email.value, password:form.password.value };
  if (mode === "register") body.confirm = form.confirm.value;

  const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
    method:"POST", headers:{ "Content-Type":"application/json" }, body:JSON.stringify(body)
  });
  if (!res.ok) return alert(await res.text());
  const { token } = await res.json();
  localStorage.setItem(TOKEN_KEY, token);
  location.href = "dashboard.html";
};
