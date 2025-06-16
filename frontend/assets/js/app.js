// frontend/assets/js/app.js  (versión completa con fix de peso y nueva vista stats)
const API_BASE  = "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";

/* helpers */
const headers = () => ({
  "Content-Type": "application/json",
  Authorization  : `Bearer ${localStorage.getItem(TOKEN_KEY)}`
});
const qs     = (s,e=document)=>e.querySelector(s);
const create = (t,c="")=>{const x=document.createElement(t);if(c)x.className=c;return x;};

/* login / register */
if(location.pathname.endsWith("/index.html") || location.pathname==="/"){authPage();}
else{dashboard();}

function authPage(){
  const f=qs("#auth-form"), c=qs("#confirm"), t=qs("#toggle-link");
  let mode="login";
  t.onclick=e=>{e.preventDefault();swap();};
  function swap(){
    mode=mode==="login"?"register":"login";
    c.classList.toggle("hidden",mode==="login");
    qs("#form-title").textContent=mode==="login"?"Iniciar sesión":"Crear cuenta";
    qs("#submit-btn").textContent=mode==="login"?"Entrar":"Registrar";
  }
  f.onsubmit=async e=>{
    e.preventDefault();
    const body={email:f.email.value,password:f.password.value};
    if(mode==="register") body.confirm=f.confirm.value;
    const r=await fetch(`${API_BASE}/api/auth/${mode}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});
    if(!r.ok) return alert(await r.text());
    localStorage.setItem(TOKEN_KEY,(await r.json()).token);
    location.href="dashboard.html";
  };
}

/* dashboard */
async function dashboard(){
  if(!localStorage.getItem(TOKEN_KEY)) return location.href="index.html";

  const templates={
    profile :qs("#profile-view"),
    stats   :qs("#stats-view"),
    machines:qs("#machines-view"),
    daily   :qs("#daily-view"),
    reports :qs("#reports-view")
  };
  const cont=qs("#view-container");
  window.addEventListener("hashchange",render);render();

  async function render(){
    const v=location.hash.slice(1)||"profile";
    cont.innerHTML="";cont.appendChild(templates[v].content.cloneNode(true));
    if(v==="profile") profileView();
    if(v==="stats")   statsView();
    if(v==="machines")machinesView();
    if(v==="daily")   dailyView();
    if(v==="reports") reportsView();
  }

  /* --- perfil --- */
  async function profileView(){
    const res=await fetch(`${API_BASE}/api/profile`,{headers:headers()});
    const d = await res.json();
    const f = qs("#profile-form");
    ["firstName","lastName","age","height"].forEach(id=>f[id].value=d[id]??"");
    f.weight.value = d.weightKg ?? "";
    f.onsubmit=async e=>{
      e.preventDefault();
      const body={
        firstName:f.firstName.value,lastName:f.lastName.value,age:f.age.value,
        heightCm:parseFloat(f.height.value),weightKg:parseFloat(f.weight.value)
      };
      await fetch(`${API_BASE}/api/profile`,{method:"PUT",headers:headers(),body:JSON.stringify(body)});
      alert("Perfil actualizado");
    };
  }

  /* --- medidas + fotos --- */
  async function statsView(){
    const f=qs("#stats-form");
    f["stats-date"].value=new Date().toISOString().slice(0,10);
    const last=await (await fetch(`${API_BASE}/api/stats/latest`,{headers:headers()})).json()||{};
    if(last.weightKg!=null)        f["stats-weight"].value=last.weightKg;
    const map={neck:"neck",chest:"chest",waist:"waist",lowerAbs:"lowerAbs",hip:"hip",
      biceps:"biceps",bicepsFlex:"bicepsFlex",forearm:"forearm",thigh:"thigh",calf:"calf"};
    Object.entries(map).forEach(([k,id])=>{
      if(last[k+"Cm"]!=null) f[`stats-${id}`].value=last[k+"Cm"];
    });
    f.onsubmit=async e=>{
      e.preventDefault();
      const dto={
        date:f["stats-date"].value,
        weightKg:parseFloat(f["stats-weight"].value),
        neckCm:parseFloat(f["stats-neck"].value),
        chestCm:parseFloat(f["stats-chest"].value),
        waistCm:parseFloat(f["stats-waist"].value),
        lowerAbsCm:parseFloat(f["stats-lowerAbs"].value),
        hipCm:parseFloat(f["stats-hip"].value),
        bicepsCm:parseFloat(f["stats-biceps"].value),
        bicepsFlexCm:parseFloat(f["stats-bicepsFlex"].value),
        forearmCm:parseFloat(f["stats-forearm"].value),
        thighCm:parseFloat(f["stats-thigh"].value),
        calfCm:parseFloat(f["stats-calf"].value)
      };
      const fd=new FormData();
      fd.append("data",new Blob([JSON.stringify(dto)],{type:"application/json"}));
      if(f["stats-front"].files[0]) fd.append("front",f["stats-front"].files[0]);
      if(f["stats-side"].files[0])  fd.append("side", f["stats-side"].files[0]);
      if(f["stats-back"].files[0])  fd.append("back", f["stats-back"].files[0]);
      await fetch(`${API_BASE}/api/stats`,{method:"POST",headers:{Authorization:headers().Authorization},body:fd});
      alert("Medidas guardadas");
    };
  }

  /* --- máquinas --- */
  async function machinesView(){
    const tbl=qs("#machine-table");
    render(await get());
    qs("#machine-form").onsubmit=async e=>{
      e.preventDefault();
      const b={
        name:qs("#machine-name").value,
        weightKg:parseFloat(qs("#machine-kg").value),
        reps:parseInt(qs("#machine-reps").value),
        sets:parseInt(qs("#machine-sets").value)
      };
      await fetch(`${API_BASE}/api/machines`,{method:"POST",headers:headers(),body:JSON.stringify(b)});
      e.target.reset();render(await get());
    };
    async function get(){return(await (await fetch(`${API_BASE}/api/machines`,{headers:headers()})).json());}
    function render(rows){
      tbl.innerHTML="";
      rows.forEach(m=>{
        const tr=create("tr");
        tr.innerHTML=`<td>${m.machine.name}</td><td>${m.weightKg}</td><td>${m.reps}</td><td>${m.sets}</td>\
          <td class="text-right"><button class="btn-danger" data-id="${m.id}">×</button></td>`;
        tbl.appendChild(tr);
        tr.querySelector("button").onclick=async()=>{
          await fetch(`${API_BASE}/api/machines/${m.id}`,{method:"DELETE",headers:headers()});
          tr.remove();
        };
      });
    }
  }

  /* --- diario --- */
  async function dailyView(){
    qs("#entry-date").value=new Date().toISOString().slice(0,10);
    const machines=await (await fetch(`${API_BASE}/api/machines`,{headers:headers()})).json();
    const cont=qs("#daily-machines");cont.innerHTML="";
    machines.forEach(m=>{
      const div=create("div","flex gap-2");
      div.innerHTML=`<span class="flex-1">${m.machine.name}</span>\
        <input type="number" class="input" style="width:6rem" value="${m.weightKg}" data-id="${m.machine.id}" data-f="kg">\
        <input type="number" class="input" style="width:5rem" value="${m.reps}" data-id="${m.machine.id}" data-f="reps">\
        <input type="number" class="input" style="width:5rem" value="${m.sets}" data-id="${m.machine.id}" data-f="sets">`;
      cont.appendChild(div);
    });
    qs("#daily-form").onsubmit=async e=>{
      e.preventDefault();
      const map={};cont.querySelectorAll("input").forEach(i=>{
        const o=map[i.dataset.id]||{};o[i.dataset.f]=parseFloat(i.value);map[i.dataset.id]=o;
      });
      const exercises=Object.entries(map).map(([id,o])=>{
        return{
          name:machines.find(x=>x.machine.id==id).machine.name,
          weightKg:o.kg,reps:o.reps,sets:o.sets
        };
      });
      await fetch(`${API_BASE}/api/daily`,{
        method:"POST",headers:headers(),body:JSON.stringify({date:qs("#entry-date").value,exercises})
      });
      alert("Registro guardado");
    };
  }

  /* --- informes --- */
  function reportsView(){
    qs("#full-pdf").onclick = ()=>window.open(`${API_BASE}/api/report/full`);
    qs("#range-pdf").onclick=()=>{
      const f=qs("#from").value,t=qs("#to").value;
      if(!f||!t) return alert("Seleccione ambas fechas");
      window.open(`${API_BASE}/api/report/period?from=${f}&to=${t}`);
    };
  }

  qs("#logout").onclick=()=>{localStorage.removeItem(TOKEN_KEY);location.href="index.html";};
}
