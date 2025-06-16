/* --------- Config --------- */
const API_BASE  = "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* --------- Helpers --------- */
const headers = () => ({
  "Content-Type": "application/json",
  Authorization  : `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});
const qs     = (sel, el = document) => el.querySelector(sel);
const create = (tag, cls = "") => { const e=document.createElement(tag); if(cls) e.className=cls; return e; };

/* ----------  Auth page ---------- */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

function authPage() {
  const form = qs("#auth-form"), confirm = qs("#confirm"), toggle = qs("#toggle-link");
  let mode = "login";

  toggle.onclick = e => { e.preventDefault(); swap(); };

  function swap(){
    mode = mode === "login" ? "register" : "login";
    confirm.classList.toggle("hidden", mode==="login");
    qs("#form-title").textContent = mode==="login" ? "Iniciar sesión" : "Crear cuenta";
    qs("#submit-btn").textContent = mode==="login" ? "Entrar"          : "Registrar";
  }

  form.onsubmit = async e => {
    e.preventDefault();
    const body = {
      email   : form.email.value,
      password: form.password.value
    };
    if(mode==="register") body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method:"POST", headers:{ "Content-Type":"application/json" }, body:JSON.stringify(body)
    });
    if(!res.ok) return alert(await res.text());

    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = "dashboard.html";
  };
}

/* ----------  Dashboard ---------- */
async function dashboard(){

  if(!localStorage.getItem(TOKEN_KEY)) return location.href="index.html";

  const templates={
    profile : qs("#profile-view"),
    machines: qs("#machines-view"),
    daily   : qs("#daily-view"),
    reports : qs("#reports-view")
  };
  const container = qs("#view-container");
  window.addEventListener("hashchange", render);
  render();

  /* ---- render view ---- */
  async function render(){
    const view = location.hash.slice(1) || "profile";
    container.innerHTML="";
    container.appendChild(templates[view].content.cloneNode(true));

    switch(view){
      case "profile": await profileView(); break;
      case "machines":machinesView(); break;
      case "daily":   dailyView();   break;
      case "reports": reportsView(); break;
    }
  }

  /* ---- perfil ---- */
  async function profileView(){
    const res  = await fetch(`${API_BASE}/api/profile`, { headers: headers() });
    const data = await res.json();
    const f = qs("#profile-form");

    // rellenar
    ["firstName","lastName","age","height","weight","neck","chest","waist",
     "lowerAbs","hip","biceps","bicepsFlex","forearm","thigh","calf"].forEach(id=>{
      if(f[id]) f[id].value = data[id+"Cm"] ?? data[id] ?? "";
    });
    // fotos preview (opcional)

    f.onsubmit = async e=>{
      e.preventDefault();
      const body={
        firstName:f.firstName.value,lastName:f.lastName.value,age:f.age.value,
        heightCm:f.height.value,     weightKg:f.weight.value,
        neckCm:f.neck.value,         chestCm:f.chest.value,  waistCm:f.waist.value,
        lowerAbsCm:f.lowerAbs.value, hipCm:f.hip.value,
        bicepsCm:f.biceps.value,     bicepsFlexCm:f.bicepsFlex.value,
        forearmCm:f.forearm.value,   thighCm:f.thigh.value,  calfCm:f.calf.value
      };
      await fetch(`${API_BASE}/api/profile`,{
        method:"PUT",headers:headers(),body:JSON.stringify(body)});
      // fotos
      await uploadPhoto("FRONT",f["photo-front"].files[0]);
      await uploadPhoto("SIDE", f["photo-side"].files[0]);
      await uploadPhoto("BACK", f["photo-back"].files[0]);
      alert("Perfil actualizado");
    };

    async function uploadPhoto(type,file){
      if(!file) return;
      const fd=new FormData();
      fd.append("type",type); fd.append("file",file);
      await fetch(`${API_BASE}/api/photos`,{method:"POST",headers:{ Authorization: headers().Authorization },body:fd});
    }
  }

  /* ---- máquinas ---- */
  async function machinesView(){
    const table=qs("#machine-table");
    renderRows(await fetchList());

    const form=qs("#machine-form");
    form.onsubmit = async e=>{
      e.preventDefault();
      const body={
        name     :form["machine-name"].value,
        weightKg :form["machine-kg"].value,
        reps     :form["machine-reps"].value,
        sets     :form["machine-sets"].value
      };
      await fetch(`${API_BASE}/api/machines`,{method:"POST",headers:headers(),body:JSON.stringify(body)});
      form.reset();
      renderRows(await fetchList());
    };

    async function fetchList(){
      return (await (await fetch(`${API_BASE}/api/machines`,{headers:headers()})).json());
    }
    function renderRows(rows){
      table.innerHTML="";
      rows.forEach(m=>{
        const tr=create("tr");
        tr.innerHTML=
          `<td>${m.machine.name}</td><td>${m.weightKg}</td><td>${m.reps}</td><td>${m.sets}</td>\
           <td class="text-right"><button class="btn-danger" data-id="${m.id}">×</button></td>`;
        table.appendChild(tr);
        tr.querySelector("button").onclick = async()=>{
          await fetch(`${API_BASE}/api/machines/${m.id}`,{method:"DELETE",headers:headers()});
          tr.remove();
        };
      });
    }
  }

  /* ---- diario ---- */
  async function dailyView(){
    const dateInput=qs("#entry-date");
    dateInput.value=new Date().toISOString().slice(0,10);

    const machines=await (await fetch(`${API_BASE}/api/machines`,{headers:headers()})).json();
    const container=qs("#daily-machines");
    container.innerHTML="";
    machines.forEach(m=>{
      const div=create("div","flex gap-2");
      div.innerHTML=
        `<span class="flex-1">${m.machine.name}</span>\
         <input type="number" class="input" style="width:6rem" value="${m.weightKg}" data-id="${m.machine.id}" data-field="kg">\
         <input type="number" class="input" style="width:5rem" value="${m.reps||10}" data-id="${m.machine.id}" data-field="reps">\
         <input type="number" class="input" style="width:5rem" value="${m.sets||3}"  data-id="${m.machine.id}" data-field="sets">`;
      container.appendChild(div);
    });

    qs("#daily-form").onsubmit = async e=>{
      e.preventDefault();
      const details=[];
      container.querySelectorAll("input").forEach(inp=>{
        const id=inp.dataset.id;
        let obj = details.find(o=>o.id===id);
        if(!obj){ obj={id,kg:null,reps:null,sets:null}; details.push(obj);}
        if(inp.dataset.field==="kg")   obj.kg   = parseFloat(inp.value);
        if(inp.dataset.field==="reps") obj.reps = parseInt(inp.value);
        if(inp.dataset.field==="sets") obj.sets = parseInt(inp.value);
      });

      const exercises = details.map(o=>({
        name: machines.find(x=>x.machine.id==o.id).machine.name,
        weightKg:o.kg, reps:o.reps, sets:o.sets
      }));

      await fetch(`${API_BASE}/api/daily`,{
        method:"POST",headers:headers(),
        body:JSON.stringify({ date:dateInput.value, exercises })
      });
      alert("Registro guardado");
    };
  }

  /* ---- informes ---- */
  function reportsView(){
    qs("#full-pdf").onclick = () =>
      window.open(`${API_BASE}/api/report/full`);

    qs("#range-pdf").onclick = () =>{
      const f=qs("#from").value, t=qs("#to").value;
      if(!f||!t) return alert("Seleccione ambas fechas");
      window.open(`${API_BASE}/api/report/period?from=${f}&to=${t}`);
    };
  }

  /* logout */
  qs("#logout").onclick = ()=>{
    localStorage.removeItem(TOKEN_KEY);
    location.href="index.html";
  };
}
