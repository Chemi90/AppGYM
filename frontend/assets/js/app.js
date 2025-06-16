/* =========================================================================
   CONFIG
   ========================================================================= */
const API_BASE = import.meta?.env?.VITE_API_BASE
               || "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* =========================================================================
   HELPERS
   ========================================================================= */
const qs     = (sel, el = document) => el.querySelector(sel);
const create = (tag, cls = "") => { const e = document.createElement(tag); if (cls) e.className = cls; return e; };

const authHeaders = () => ({
  "Content-Type": "application/json",
  Authorization  : `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});

/* =========================================================================
   ROUTING
   ========================================================================= */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

/* =========================================================================
   AUTH PAGE
   ========================================================================= */
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
      email   : form.email.value,
      password: form.password.value
    };
    if (mode === "register") body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method : "POST",
      headers: { "Content-Type": "application/json" },
      body   : JSON.stringify(body)
    });

    if (!res.ok) return alert(await res.text());

    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = "dashboard.html";
  };
}

/* =========================================================================
   DASHBOARD
   ========================================================================= */
async function dashboard() {
  if (!localStorage.getItem(TOKEN_KEY)) return location.href = "index.html";

  const container = qs("#view-container");
  const templates = {
    profile : qs("#profile-view"),
    machines: qs("#machines-view"),
    daily   : qs("#daily-view")
  };

  window.addEventListener("hashchange", render);
  render();

  /* -------- render según hash -------- */
  async function render() {
    const view = location.hash.slice(1) || "profile";
    container.innerHTML = "";
    container.appendChild(templates[view].content.cloneNode(true));

    if (view === "profile")  profileView();
    if (view === "machines") machinesView();
    if (view === "daily")    dailyView();
    reports();                                  // botones PDF disponibles en cualquier vista
  }

  /* -----------------------------------------------------------------------
     PROFILE
     ----------------------------------------------------------------------- */
  async function profileView() {
    const res  = await fetch(`${API_BASE}/api/profile`, { headers: authHeaders() });
    const data = await res.json();
    const form = qs("#profile-form");

    /* rellenar inputs */
    Object.entries({
      firstName     : data.firstName,
      lastName      : data.lastName,
      age           : data.age,
      height        : data.heightCm,
      weight        : data.weightKg,
      neck          : data.neckCm,
      chest         : data.chestCm,
      waist         : data.waistCm,
      lowerAbs      : data.lowerAbsCm,
      hip           : data.hipCm,
      biceps        : data.bicepsCm,
      bicepsFlex    : data.bicepsFlexCm,
      forearm       : data.forearmCm,
      thigh         : data.thighCm,
      calf          : data.calfCm
    }).forEach(([id, val]) => { if (val !== null) form[id].value = val; });

    /* guardar */
    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        firstName  : form.firstName.value,
        lastName   : form.lastName.value,
        age        : +form.age.value,
        heightCm   : +form.height.value,
        weightKg   : +form.weight.value,
        neckCm     : +form.neck.value,
        chestCm    : +form.chest.value,
        waistCm    : +form.waist.value,
        lowerAbsCm : +form.lowerAbs.value,
        hipCm      : +form.hip.value,
        bicepsCm   : +form.biceps.value,
        bicepsFlexCm:+form.bicepsFlex.value,
        forearmCm  : +form.forearm.value,
        thighCm    : +form.thigh.value,
        calfCm     : +form.calf.value
      };
      await fetch(`${API_BASE}/api/profile`, {
        method : "PUT",
        headers: authHeaders(),
        body   : JSON.stringify(body)
      });
      alert("Perfil actualizado");
    };
  }

  /* -----------------------------------------------------------------------
     MACHINES
     ----------------------------------------------------------------------- */
  async function machinesView() {
    const table = qs("#machine-table");
    const list  = await (await fetch(`${API_BASE}/api/machines`, { headers: authHeaders() })).json();
    renderRows(list);

    const form = qs("#machine-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        name     : form["machine-name"].value,
        weightKg : +form["machine-weight"].value,
        reps     : +form["machine-reps"].value,
        sets     : +form["machine-sets"].value
      };
      await fetch(`${API_BASE}/api/machines`, {
        method : "POST",
        headers: authHeaders(),
        body   : JSON.stringify(body)
      });
      form.reset();
      renderRows(await (await fetch(`${API_BASE}/api/machines`, { headers: authHeaders() })).json());
    };

    function renderRows(rows) {
      table.innerHTML = "";
      rows.forEach(m => {
        const tr = create("tr");
        tr.innerHTML =
          `<td>${m.name}</td>\
           <td>${m.weightKg}</td>\
           <td>${m.reps}</td>\
           <td>${m.sets}</td>\
           <td class="text-right"><button data-id="${m.id}" class="btn-danger">×</button></td>`;
        table.appendChild(tr);
        tr.querySelector("button").onclick = async () => {
          await fetch(`${API_BASE}/api/machines/${m.id}`, { method:"DELETE", headers:authHeaders() });
          tr.remove();
        };
      });
    }
  }

  /* -----------------------------------------------------------------------
     DAILY
     ----------------------------------------------------------------------- */
  async function dailyView() {
    const dateInput = qs("#entry-date");
    dateInput.value = new Date().toISOString().slice(0,10);

    const machines  = await (await fetch(`${API_BASE}/api/machines`, { headers: authHeaders() })).json();
    const container = qs("#daily-machines");
    container.innerHTML = "";
    machines.forEach(m => {
      const div = create("div","flex gap-2");
      div.innerHTML = `<span class="flex-1">${m.name}</span>\
        <input type="number" class="input w-24" value="${m.weightKg}" data-id="${m.id}">\
        <input type="number" class="input w-16" value="${m.reps}"      data-r="reps">\
        <input type="number" class="input w-16" value="${m.sets}"      data-r="sets">`;
      container.appendChild(div);
    });

    const form = qs("#daily-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const details = {};
      container.querySelectorAll("div").forEach(row => {
        const id   = row.querySelector("[data-id]").dataset.id;
        const kg   = +row.querySelector("[data-id]").value;
        const reps = +row.querySelector("[data-r='reps']").value;
        const sets = +row.querySelector("[data-r='sets']").value;
        details[id] = { weightKg: kg, reps, sets };
      });
      await fetch(`${API_BASE}/api/daily`, {
        method : "POST",
        headers: authHeaders(),
        body   : JSON.stringify({ date: dateInput.value, exercises: Object.values(details) })
      });
      alert("Registro guardado");
    };
  }

  /* -----------------------------------------------------------------------
     REPORTS (botones de descarga)
     ----------------------------------------------------------------------- */
  function reports(){
    const fullBtn   = qs("#full-pdf");
    const rangeBtn  = qs("#range-pdf");
    if (fullBtn) fullBtn.onclick = () =>
      download(`${API_BASE}/api/report/full`, "progreso.pdf");

    if (rangeBtn) rangeBtn.onclick = () => {
      const f = qs("#from").value, t = qs("#to").value;
      if (!f || !t) return alert("Seleccione ambas fechas");
      download(`${API_BASE}/api/report/period?from=${f}&to=${t}`,
               `progreso_${f}_${t}.pdf`);
    };
  }

  /* ---------- descarga con token en query-string ---------- */
  function download(url, filename){
    const token = localStorage.getItem(TOKEN_KEY);
    const href  = `${url}${url.includes("?")?"&":"?"}token=${token}`;
    window.open(href, "_blank");
  }

  /* ---------- logout ---------- */
  qs("#logout").onclick = () => {
    localStorage.removeItem(TOKEN_KEY);
    location.href = "index.html";
  };
}
