/* --------- Config --------- */
const API_BASE = "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* --------- Helpers --------- */
const headers = () => ({
  "Content-Type": "application/json",
  Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});
const qs = (sel, el = document) => el.querySelector(sel);
const create = (tag, cls = "") => {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  return e;
};

/* ----------  Auth page ---------- */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

function authPage() {
  const form    = qs("#auth-form");
  const confirm = qs("#confirm");
  const toggle  = qs("#toggle-link");
  let mode = "login";

  toggle.onclick = e => { e.preventDefault(); swap(); };

  function swap() {
    mode = mode === "login" ? "register" : "login";
    confirm.classList.toggle("hidden", mode === "login");
    qs("#form-title").textContent = mode === "login" ? "Iniciar sesión" : "Crear cuenta";
    qs("#submit-btn").textContent = mode === "login" ? "Entrar" : "Registrar";
  }

  form.onsubmit = async e => {
    e.preventDefault();

    const body = {
      email:    form.email.value,
      password: form.password.value
    };
    if (mode === "register") body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    if (!res.ok) return alert(await res.text());
    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = "dashboard.html";
  };
}

/* ----------  Dashboard page ---------- */
async function dashboard() {
  if (!localStorage.getItem(TOKEN_KEY)) return location.href = "index.html";

  const container = qs("#view-container");
  const templates = {
    profile:  qs("#profile-view"),
    machines: qs("#machines-view"),
    daily:    qs("#daily-view")
  };

  window.addEventListener("hashchange", render);
  render();

  async function render() {
    const view = location.hash.slice(1) || "profile";
    container.innerHTML = "";
    container.appendChild(templates[view].content.cloneNode(true));
    if (view === "profile")  profileView();
    if (view === "machines") machinesView();
    if (view === "daily")    dailyView();
  }

  /* ------- profile ------- */
  async function profileView() {
    const res  = await fetch(`${API_BASE}/api/profile`, { headers: headers() });
    const data = await res.json();
    const form = qs("#profile-form");
    form.firstName.value   = data.firstName   || "";
    form.lastName.value    = data.lastName    || "";
    form.age.value         = data.age         || "";
    form.height.value      = data.heightCm    || "";
    form.weight.value      = data.weightKg    || "";
    form.measurements.value= data.measurements|| "";

    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        firstName:   form.firstName.value,
        lastName:    form.lastName.value,
        age:         form.age.value,
        heightCm:    form.height.value,
        weightKg:    form.weight.value,
        measurements:form.measurements.value
      };
      await fetch(`${API_BASE}/api/profile`, {
        method:"PUT", headers:headers(), body:JSON.stringify(body)
      });
      alert("Perfil actualizado");
    };
  }

  /* ------- machines ------- */
  async function machinesView() {
    const table = qs("#machine-table");
    const list  = await (await fetch(`${API_BASE}/api/machines`, { headers: headers() })).json();
    renderRows(list);

    const form = qs("#machine-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const body = { name: form["machine-name"].value, weightKg: form["machine-weight"].value };
      await fetch(`${API_BASE}/api/machines`, {
        method:"POST", headers:headers(), body:JSON.stringify(body)
      });
      form.reset();
      renderRows(await (await fetch(`${API_BASE}/api/machines`, { headers: headers() })).json());
    };

    function renderRows(rows) {
      table.innerHTML = "";
      rows.forEach(m => {
        const tr = create("tr");
        tr.innerHTML =
          `<td>${m.name}</td><td>${m.weightKg}</td>\
           <td class="text-right"><button data-id="${m.id}" class="btn-danger">×</button></td>`;
        table.appendChild(tr);
        tr.querySelector("button").onclick = async () => {
          await fetch(`${API_BASE}/api/machines/${m.id}`, { method:"DELETE", headers:headers() });
          tr.remove();
        };
      });
    }
  }

  /* ------- daily ------- */
  async function dailyView() {
    const dateInput = qs("#entry-date");
    dateInput.value = new Date().toISOString().slice(0,10);

    const machines  = await (await fetch(`${API_BASE}/api/machines`, { headers: headers() })).json();
    const container = qs("#daily-machines");
    container.innerHTML = "";
    machines.forEach(m => {
      const div = create("div", "flex gap-2");
      div.innerHTML = `<span class="flex-1">${m.name}</span>\
        <input type="number" class="input" style="width:6rem;" value="${m.weightKg}" data-id="${m.id}">`;
      container.appendChild(div);
    });

    const form = qs("#daily-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const details = {};
      container.querySelectorAll("input").forEach(inp => {
        details[inp.dataset.id] = parseFloat(inp.value);
      });
      await fetch(`${API_BASE}/api/daily`, {
        method:"POST", headers:headers(),
        body:JSON.stringify({ date: dateInput.value, details })
      });
      alert("Registro guardado");
    };
  }

  /* ------- logout ------- */
  qs("#logout").onclick = () => {
    localStorage.removeItem(TOKEN_KEY);
    location.href = "index.html";
  };
}
