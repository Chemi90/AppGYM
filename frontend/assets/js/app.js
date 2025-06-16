/* =========================================================================
   CONFIG
   ========================================================================= */
const API_BASE  = import.meta?.env?.VITE_API_BASE
               || "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* =========================================================================
   HELPERS
   ========================================================================= */
const qs     = (sel, el = document) => el.querySelector(sel);
const create = (tag, cls = "") => {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  return e;
};
const authHeaders = () => ({
  "Content-Type": "application/json",
  Authorization  : `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});

/* =========================================================================
   ROUTING (index ↔ dashboard)
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
  let mode      = "login";

  toggle.onclick = e => { e.preventDefault(); swap(); };

  function swap() {
    mode = mode === "login" ? "register" : "login";
    confirm.classList.toggle("hidden", mode === "login");
    qs("#form-title").textContent = mode === "login" ? "Iniciar sesión"
                                                    : "Crear cuenta";
    qs("#submit-btn").textContent = mode === "login" ? "Entrar"
                                                    : "Registrar";
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
    stats   : qs("#stats-view"),
    machines: qs("#machines-view"),
    daily   : qs("#daily-view"),
    reports : qs("#reports-view")
  };

  window.addEventListener("hashchange", render);
  render();

  /* ------------- router interno ------------- */
  async function render() {
    const view = location.hash.slice(1) || "profile";
    container.innerHTML = "";
    container.appendChild(templates[view].content.cloneNode(true));

    if (view === "profile")  profileView();
    if (view === "stats")    statsView();
    if (view === "machines") machinesView();
    if (view === "daily")    dailyView();
    if (view === "reports")  reports();
  }

  /* -----------------------------------------------------------------------
     PROFILE
     ----------------------------------------------------------------------- */
  async function profileView() {
    const res  = await fetch(`${API_BASE}/api/profile`, { headers: authHeaders() });
    const data = await res.json();
    const form = qs("#profile-form");

    /* rellenar (solo los campos presentes) */
    [
      "firstName","lastName","age","height","weight",
      "neck","chest","waist","lowerAbs","hip",
      "biceps","bicepsFlex","forearm","thigh","calf"
    ].forEach(k => { if (data[k+"Cm"] ?? data[k]) form[k].value = data[k+"Cm"] ?? data[k] });

    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        firstName  : form.firstName.value,
        lastName   : form.lastName.value,
        age        : +form.age.value,
        heightCm   : +form.height.value,
        weightKg   : +form.weight.value
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
     STATS  (MEDIDAS + FOTOS)
     ----------------------------------------------------------------------- */
  async function statsView() {
    const form = qs("#stats-form");
    const today = new Date().toISOString().slice(0,10);
    qs("#stats-date").value = today;                        // por defecto hoy

    /* -------- carga última medición -------- */
    const last = await fetch(`${API_BASE}/api/stats`, { headers: authHeaders() })
                        .then(r => r.ok ? r.json() : null)
                        .catch(()=>null);
    if (last) {
      const map = {
        weightKg    :"stats-weight",
        neckCm      :"stats-neck",
        chestCm     :"stats-chest",
        waistCm     :"stats-waist",
        lowerAbsCm  :"stats-lowerAbs",
        hipCm       :"stats-hip",
        bicepsCm    :"stats-biceps",
        bicepsFlexCm:"stats-bicepsFlex",
        forearmCm   :"stats-forearm",
        thighCm     :"stats-thigh",
        calfCm      :"stats-calf"
      };
      Object.entries(map).forEach(([k,id])=>{
        if (last[k] != null) form[id].value = last[k];
      });
    }

    /* -------- guardar -------- */
    form.onsubmit = async e => {
      e.preventDefault();

      /* ―― JSON de medidas ―― */
      const jsonBody = {
        date        : form["stats-date"].value,
        weightKg    : +form["stats-weight"].value    || null,
        neckCm      : +form["stats-neck"].value      || null,
        chestCm     : +form["stats-chest"].value     || null,
        waistCm     : +form["stats-waist"].value     || null,
        lowerAbsCm  : +form["stats-lowerAbs"].value  || null,
        hipCm       : +form["stats-hip"].value       || null,
        bicepsCm    : +form["stats-biceps"].value    || null,
        bicepsFlexCm: +form["stats-bicepsFlex"].value|| null,
        forearmCm   : +form["stats-forearm"].value   || null,
        thighCm     : +form["stats-thigh"].value     || null,
        calfCm      : +form["stats-calf"].value      || null
      };

      /* medida */
      const res = await fetch(`${API_BASE}/api/stats`, {
        method : "POST",
        headers: authHeaders(),
        body   : JSON.stringify(jsonBody)
      });
      if (!res.ok) return alert("Error al guardar medidas");

      /* fotos (si las hay) */
      const photos = [
        { id:"stats-front", type:"FRONT" },
        { id:"stats-side" , type:"SIDE"  },
        { id:"stats-back" , type:"BACK"  }
      ];
      for (const p of photos){
        const file = form[p.id].files[0];
        if (!file) continue;
        const fd = new FormData();
        fd.append("type", p.type);
        fd.append("file", file);
        await fetch(`${API_BASE}/api/photos`, {
          method : "POST",
          headers: { "Authorization": authHeaders().Authorization },
          body   : fd
        });
      }
      alert("Medidas/fotos guardadas");
      form.reset();
      qs("#stats-date").value = today;
    };
  }

  /* -----------------------------------------------------------------------
     MACHINES
     ----------------------------------------------------------------------- */
  async function machinesView() {
    const table = qs("#machine-table");
    renderRows(await fetchList());

    const form = qs("#machine-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        name     : form["machine-name"].value,
        weightKg : +form["machine-kg"].value,
        reps     : +form["machine-reps"].value,
        sets     : +form["machine-sets"].value
      };
      await fetch(`${API_BASE}/api/machines`, {
        method : "POST",
        headers: authHeaders(),
        body   : JSON.stringify(body)
      });
      form.reset();
      renderRows(await fetchList());
    };

    async function fetchList(){
      return await (await fetch(`${API_BASE}/api/machines`, { headers: authHeaders() })).json();
    }
    function renderRows(rows) {
      table.innerHTML = "";
      rows.forEach(m => {
        const tr = create("tr");
        tr.innerHTML =
          `<td>${m.name}</td>\
           <td>${m.weightKg ?? "-"}</td>\
           <td>${m.reps ?? "-"}</td>\
           <td>${m.sets ?? "-"}</td>\
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
      const row = create("div","flex gap-2");
      row.innerHTML = `<span class="flex-1">${m.name}</span>\
        <input type="number" class="input w-24" value="${m.weightKg ?? 0}" data-id="${m.id}">\
        <input type="number" class="input w-16" value="${m.reps    ?? 0}" data-r="reps">\
        <input type="number" class="input w-16" value="${m.sets    ?? 0}" data-r="sets">`;
      container.appendChild(row);
    });

    qs("#daily-form").onsubmit = async e => {
      e.preventDefault();
      const exercises = [];
      container.querySelectorAll("div").forEach(row => {
        exercises.push({
          name     : row.children[0].textContent.trim(),
          weightKg : +row.querySelector("[data-id]").value,
          reps     : +row.querySelector("[data-r='reps']").value,
          sets     : +row.querySelector("[data-r='sets']").value
        });
      });
      await fetch(`${API_BASE}/api/daily`, {
        method : "POST",
        headers: authHeaders(),
        body   : JSON.stringify({ date: dateInput.value, exercises })
      });
      alert("Registro diario guardado");
    };
  }

  /* -----------------------------------------------------------------------
     REPORTS – descarga PDF
     ----------------------------------------------------------------------- */
  function reports() {
    const fullBtn  = qs("#full-pdf");
    const rangeBtn = qs("#range-pdf");

    if (fullBtn)
      fullBtn.onclick = () =>
        download(`${API_BASE}/api/report/full`, "progreso.pdf");

    if (rangeBtn)
      rangeBtn.onclick = () => {
        const f = qs("#from").value, t = qs("#to").value;
        if (!f || !t) return alert("Seleccione ambas fechas");
        download(`${API_BASE}/api/report/period?from=${f}&to=${t}`,
                 `progreso_${f}_${t}.pdf`);
      };
  }

  /* helper descarga con token query-string */
  function download(url, filename) {
    const token = localStorage.getItem(TOKEN_KEY);
    const href  = `${url}${url.includes("?")?"&":"?"}token=${token}`;
    window.open(href, "_blank");
  }

  /* logout */
  qs("#logout").onclick = () => {
    localStorage.removeItem(TOKEN_KEY);
    location.href = "index.html";
  };
}
