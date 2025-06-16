/* Gym-Tracker  –  Frontend SPA  */
/* --------- Config --------- */
const API_BASE  = "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* --------- Helpers --------- */
const headers = () => ({
  "Content-Type": "application/json",
  Authorization  : `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});
const qs     = (sel, el = document) => el.querySelector(sel);
const create = (tag, cls = "") => { const e = document.createElement(tag); if (cls) e.className = cls; return e; };

/* ----------  Login / Registro ---------- */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

/* =======  pantalla de acceso  ======= */
function authPage() {
  const form = qs("#auth-form"), confirm = qs("#confirm"), toggle = qs("#toggle-link");
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
    const body = { email: form.email.value, password: form.password.value };
    if (mode === "register") body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (!res.ok) return alert(await res.text());
    localStorage.setItem(TOKEN_KEY, (await res.json()).token);
    location.href = "dashboard.html";
  };
}

/* =======  dashboard SPA  ======= */
async function dashboard() {

  if (!localStorage.getItem(TOKEN_KEY)) return location.href = "index.html";

  const views = {
    profile : qs("#profile-view"),
    stats   : qs("#stats-view"),
    machines: qs("#machines-view"),
    daily   : qs("#daily-view"),
    reports : qs("#reports-view")
  };
  const container = qs("#view-container");
  window.addEventListener("hashchange", render); render();

  async function render() {
    const v = location.hash.slice(1) || "profile";
    container.innerHTML = "";
    container.appendChild(views[v].content.cloneNode(true));
    if (v === "profile")  profileView();
    if (v === "stats")    statsView();
    if (v === "machines") machinesView();
    if (v === "daily")    dailyView();
    if (v === "reports")  reportsView();
  }

  /* ---- Perfil (corrige height) ---- */
  async function profileView() {
    const form = qs("#profile-form");
    const data = await (await fetch(`${API_BASE}/api/profile`, { headers: headers() })).json();

    form.firstName.value = data.firstName ?? "";
    form.lastName.value  = data.lastName  ?? "";
    form.age.value       = data.age       ?? "";
    form.height.value    = data.heightCm  ?? "";
    form.weight.value    = data.weightKg  ?? "";

    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        firstName: form.firstName.value,
        lastName : form.lastName.value,
        age      : parseInt(form.age.value),
        heightCm : parseFloat(form.height.value),   // ← convertido a número
        weightKg : parseFloat(form.weight.value)    // ← ya convertido antes
      };
      await fetch(`${API_BASE}/api/profile`, { method:"PUT", headers:headers(), body:JSON.stringify(body) });
      alert("Perfil actualizado");
    };
  }

  /* ---- Medidas + Fotos ---- */
  async function statsView() {
    const f = qs("#stats-form");
    f["stats-date"].value = new Date().toISOString().slice(0,10);

    const last = await (await fetch(`${API_BASE}/api/stats/latest`, { headers: headers() })).json() || {};
    if (last.weightKg      != null) f["stats-weight"].value     = last.weightKg;
    if (last.neckCm        != null) f["stats-neck"].value       = last.neckCm;
    if (last.chestCm       != null) f["stats-chest"].value      = last.chestCm;
    if (last.waistCm       != null) f["stats-waist"].value      = last.waistCm;
    if (last.lowerAbsCm    != null) f["stats-lowerAbs"].value   = last.lowerAbsCm;
    if (last.hipCm         != null) f["stats-hip"].value        = last.hipCm;
    if (last.bicepsCm      != null) f["stats-biceps"].value     = last.bicepsCm;
    if (last.bicepsFlexCm  != null) f["stats-bicepsFlex"].value = last.bicepsFlexCm;
    if (last.forearmCm     != null) f["stats-forearm"].value    = last.forearmCm;
    if (last.thighCm       != null) f["stats-thigh"].value      = last.thighCm;
    if (last.calfCm        != null) f["stats-calf"].value       = last.calfCm;

    f.onsubmit = async e => {
      e.preventDefault();
      const dto = {
        date       : f["stats-date"].value,
        weightKg   : parseFloat(f["stats-weight"].value),
        neckCm     : parseFloat(f["stats-neck"].value),
        chestCm    : parseFloat(f["stats-chest"].value),
        waistCm    : parseFloat(f["stats-waist"].value),
        lowerAbsCm : parseFloat(f["stats-lowerAbs"].value),
        hipCm      : parseFloat(f["stats-hip"].value),
        bicepsCm   : parseFloat(f["stats-biceps"].value),
        bicepsFlexCm:parseFloat(f["stats-bicepsFlex"].value),
        forearmCm  : parseFloat(f["stats-forearm"].value),
        thighCm    : parseFloat(f["stats-thigh"].value),
        calfCm     : parseFloat(f["stats-calf"].value)
      };
      const fd = new FormData();
      fd.append("data", new Blob([JSON.stringify(dto)], { type:"application/json" }));
      if (f["stats-front"].files[0]) fd.append("front", f["stats-front"].files[0]);
      if (f["stats-side"].files[0])  fd.append("side",  f["stats-side"].files[0]);
      if (f["stats-back"].files[0])  fd.append("back",  f["stats-back"].files[0]);

      await fetch(`${API_BASE}/api/stats`, {
        method:"POST",
        headers:{ Authorization:headers().Authorization },
        body:fd
      });
      alert("Medidas guardadas");
    };
  }

  /* ---- Máquinas ---- */
  async function machinesView() {
    const tbl = qs("#machine-table");
    render(await fetchList());

    qs("#machine-form").onsubmit = async e => {
      e.preventDefault();
      const body = {
        name     : qs("#machine-name").value,
        weightKg : parseFloat(qs("#machine-kg").value),
        reps     : parseInt(qs("#machine-reps").value),
        sets     : parseInt(qs("#machine-sets").value)
      };
      await fetch(`${API_BASE}/api/machines`, { method:"POST", headers:headers(), body:JSON.stringify(body) });
      e.target.reset();
      render(await fetchList());
    };

    async function fetchList() {
      return await (await fetch(`${API_BASE}/api/machines`, { headers:headers() })).json();
    }
    function render(rows) {
      tbl.innerHTML = "";
      rows.forEach(m => {
        const tr = create("tr");
        tr.innerHTML = `<td>${m.machine.name}</td><td>${m.weightKg}</td><td>${m.reps}</td><td>${m.sets}</td>\
          <td class="text-right"><button class="btn-danger" data-id="${m.id}">×</button></td>`;
        tbl.appendChild(tr);
        tr.querySelector("button").onclick = async () => {
          await fetch(`${API_BASE}/api/machines/${m.id}`, { method:"DELETE", headers:headers() });
          tr.remove();
        };
      });
    }
  }

  /* ---- Diario ---- */
  async function dailyView() {
    qs("#entry-date").value = new Date().toISOString().slice(0,10);
    const machines = await (await fetch(`${API_BASE}/api/machines`, { headers:headers() })).json();

    const cont = qs("#daily-machines"); cont.innerHTML = "";
    machines.forEach(m => {
      const div = create("div","flex gap-2");
      div.innerHTML = `<span class="flex-1">${m.machine.name}</span>\
        <input type="number" class="input" style="width:6rem" value="${m.weightKg}" data-id="${m.machine.id}" data-f="kg">\
        <input type="number" class="input" style="width:5rem" value="${m.reps}"     data-id="${m.machine.id}" data-f="reps">\
        <input type="number" class="input" style="width:5rem" value="${m.sets}"     data-id="${m.machine.id}" data-f="sets">`;
      cont.appendChild(div);
    });

    qs("#daily-form").onsubmit = async e => {
      e.preventDefault();
      const map = {};
      cont.querySelectorAll("input").forEach(i=>{
        const o = map[i.dataset.id] || {};
        o[i.dataset.f] = parseFloat(i.value);
        map[i.dataset.id] = o;
      });
      const exercises = Object.entries(map).map(([id, o]) => ({
        name     : machines.find(x=>x.machine.id==id).machine.name,
        weightKg : o.kg, reps:o.reps, sets:o.sets
      }));
      await fetch(`${API_BASE}/api/daily`, {
        method:"POST", headers:headers(),
        body:JSON.stringify({ date:qs("#entry-date").value, exercises })
      });
      alert("Registro guardado");
    };
  }

  /* ---- Informes PDF ---- */
  function reportsView() {
    qs("#full-pdf").onclick  = () => window.open(`${API_BASE}/api/report/full`);
    qs("#range-pdf").onclick = () => {
      const f=qs("#from").value, t=qs("#to").value;
      if(!f||!t) return alert("Seleccione ambas fechas");
      window.open(`${API_BASE}/api/report/period?from=${f}&to=${t}`);
    };
  }

  /* ---- logout ---- */
  qs("#logout").onclick = () => {
    localStorage.removeItem(TOKEN_KEY);
    location.href = "index.html";
  };
}
