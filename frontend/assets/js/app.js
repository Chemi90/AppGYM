/* =========================================================================
   GYM TRACKER · Front-end (Netlify)
   Archivo: assets/js/app.js
   Incluye: Quick-Add ＋  · Timer básico  · Cronómetro / Countdown / Alarma
   ========================================================================= */

/* ------------------------------------------------------------------ CONFIG */
const API_BASE  = import.meta?.env?.VITE_API_BASE
               || "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";
const DEFAULT_REST_SEC = 90;        // timer básico por defecto (seg)

/* ---------------------------------------------------------------- HELPERS */
const qs  = (sel, el = document) => el.querySelector(sel);
const qsa = (sel, el = document) => el.querySelectorAll(sel);
const create = (tag, cls = "") => { const e=document.createElement(tag); if(cls) e.className=cls; return e; };

const authHeaders = () => ({ "Authorization": `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* ---------------------------------------------------------------- ROUTING */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

/* ========================================================================
   1) AUTH PAGE
   ======================================================================== */
function authPage(){
  const form    = qs("#auth-form");
  const confirm = qs("#confirm");
  const toggle  = qs("#toggle-link");
  let mode="login";

  toggle.onclick=e=>{e.preventDefault();swap();};
  function swap(){
    mode = mode==="login"?"register":"login";
    confirm.classList.toggle("hidden",mode==="login");
    qs("#form-title").textContent = mode==="login"?"Iniciar sesión":"Crear cuenta";
    qs("#submit-btn").textContent = mode==="login"?"Entrar":"Registrar";
  }

  form.onsubmit = async e=>{
    e.preventDefault();
    const body={email:form.email.value,password:form.password.value};
    if(mode==="register") body.confirm=form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`,{
      method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)
    });
    if(!res.ok) return alert(await res.text());

    const {token}=await res.json();
    localStorage.setItem(TOKEN_KEY,token);
    location.href="dashboard.html";
  };
}

/* ========================================================================
   2) DASHBOARD
   ======================================================================== */
async function dashboard(){
  if(!localStorage.getItem(TOKEN_KEY)) return location.href="index.html";

  const container = qs("#view-container");
  const templates = {
    profile : qs("#profile-view"),
    stats   : qs("#stats-view"),
    machines: qs("#machines-view"),
    daily   : qs("#daily-view"),
    reports : qs("#reports-view")
  };

  window.addEventListener("hashchange",render);
  render();

  /* ---------------- render segun hash ---------------- */
  async function render(){
    const view = location.hash.slice(1)||"profile";
    container.innerHTML="";
    const frag = templates[view].content.cloneNode(true);
    frag.firstElementChild?.classList.add("fade-in");
    container.appendChild(frag);

    if(view==="profile")  profileView();
    if(view==="stats")    statsView();
    if(view==="machines") machinesView();
    if(view==="daily")    dailyView();
    if(view==="reports")  reportsView();
  }

  /* ------------------------------------------------------------------
     PROFILE
     ------------------------------------------------------------------ */
  async function profileView(){
    const res  = await fetch(`${API_BASE}/api/profile`,{headers:authHeaders()});
    const data = await res.json();
    const form = qs("#profile-form");

    Object.entries({
      firstName:data.firstName,lastName:data.lastName,age:data.age,
      height:data.heightCm,weight:data.weightKg
    }).forEach(([id,val])=>{if(val!==null) form[id].value=val;});

    form.onsubmit = async e=>{
      e.preventDefault();
      const body={
        firstName:form.firstName.value,lastName:form.lastName.value,
        age:+form.age.value,heightCm:+form.height.value,weightKg:+form.weight.value
      };
      await fetch(`${API_BASE}/api/profile`,{
        method:"PUT",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify(body)
      });
      alert("Perfil actualizado");
    };
  }

  /* ------------------------------------------------------------------
     STATS
     ------------------------------------------------------------------ */
  async function statsView(){
    const form = qs("#stats-form");
    form["stats-date"].value = new Date().toISOString().slice(0,10);

    form.onsubmit = async e=>{
      e.preventDefault();
      const body={
        date:form["stats-date"].value,
        weightKg:nf(form["stats-weight"].value),
        waistCm:nf(form["stats-waist"].value),
        hipCm:nf(form["stats-hip"].value),
        thighCm:nf(form["stats-thigh"].value),
        bicepsCm:nf(form["stats-biceps"].value),
        neckCm:nf(form["stats-neck"].value),
        chestCm:nf(form["stats-chest"].value),
        lowerAbsCm:nf(form["stats-lowerAbs"].value),
        bicepsFlexCm:nf(form["stats-bicepsFlex"].value),
        forearmCm:nf(form["stats-forearm"].value),
        calfCm:nf(form["stats-calf"].value)
      };
      await fetch(`${API_BASE}/api/stats`,{
        method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify(body)
      });
      alert("Medidas guardadas");
      form.reset();
    };
  }
  const nf=v=>v===""?null:+v;

  /* ------------------------------------------------------------------
     MACHINES (edición inline)
     ------------------------------------------------------------------ */
  async function machinesView(){
    const table = qs("#machine-table");
    const list  = await (await fetch(`${API_BASE}/api/machines`,{headers:authHeaders()})).json();
    renderRows(list);

    const form = qs("#machine-form");
    form.onsubmit = async e=>{
      e.preventDefault();
      const body={
        name:form["machine-name"].value,
        weightKg:+form["machine-kg"].value,
        reps:+form["machine-reps"].value,
        sets:+form["machine-sets"].value
      };
      await fetch(`${API_BASE}/api/machines`,{
        method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},body:JSON.stringify(body)
      });
      form.reset();
      renderRows(await (await fetch(`${API_BASE}/api/machines`,{headers:authHeaders()})).json());
    };

    function renderRows(rows){
      table.innerHTML="";
      rows.forEach(m=>{
        const tr=create("tr");
        tr.innerHTML=
          `<td class="machine-name">${m.machine.name}</td>
           <td class="machine-kg">${m.weightKg}</td>
           <td class="machine-reps">${m.reps}</td>
           <td class="machine-sets">${m.sets}</td>
           <td class="text-right">
             <button class="btn-icon btn-edit" title="Editar" data-id="${m.id}">✎</button>
             <button class="btn-danger"        title="Eliminar" data-id="${m.id}">×</button>
           </td>`;
        table.appendChild(tr);

        tr.querySelector(".btn-danger").onclick = async()=>{
          await fetch(`${API_BASE}/api/machines/${m.id}`,{method:"DELETE",headers:authHeaders()});
          tr.remove();
        };
        tr.querySelector(".btn-edit").onclick = ()=>startEdit(tr,m);
      });
    }
    function startEdit(tr,m){
      const kgTd=tr.querySelector(".machine-kg"), repsTd=tr.querySelector(".machine-reps"), setsTd=tr.querySelector(".machine-sets");
      kgTd.innerHTML  =`<input type="number" class="input w-24" value="${m.weightKg}">`;
      repsTd.innerHTML=`<input type="number" class="input w-16" value="${m.reps}">`;
      setsTd.innerHTML=`<input type="number" class="input w-16" value="${m.sets}">`;
      const actions=tr.lastElementChild; actions.querySelector(".btn-edit").remove();
      const saveBtn=create("button","btn"); saveBtn.textContent="Guardar"; actions.prepend(saveBtn);
      saveBtn.onclick=()=>saveEdit(tr,m);
    }
    async function saveEdit(tr,m){
      const kg=+tr.querySelector(".machine-kg input").value,
            reps=+tr.querySelector(".machine-reps input").value,
            sets=+tr.querySelector(".machine-sets input").value;
      await fetch(`${API_BASE}/api/machines`,{
        method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},
        body:JSON.stringify({name:m.machine.name,weightKg:kg,reps,sets})
      });
      tr.querySelector(".machine-kg").textContent=kg;
      tr.querySelector(".machine-reps").textContent=reps;
      tr.querySelector(".machine-sets").textContent=sets;
      const actions=tr.lastElementChild; actions.querySelector("button.btn").remove();
      const editBtn=create("button","btn-icon btn-edit"); editBtn.textContent="✎"; editBtn.title="Editar";
      editBtn.onclick=()=>startEdit(tr,m); actions.prepend(editBtn);
    }
  }

  /* ------------------------------------------------------------------
     DAILY  (registro diario + Quick-Add + Timer básico + Advanced)
     ------------------------------------------------------------------ */
  async function dailyView(){
    const dateInput = qs("#entry-date");
    dateInput.value = new Date().toISOString().slice(0,10);

    /* ---------- TIMER BÁSICO entre series ---------- */
    const timerBox = qs("#series-timer");
    const alertSel = qs("#alert-mode");
    let timerId=null, remaining=DEFAULT_REST_SEC;

    function startBasicTimer(){
      clearInterval(timerId); remaining=DEFAULT_REST_SEC; updateBasic();
      timerId=setInterval(()=>{
        remaining--; updateBasic();
        if(remaining<=0){clearInterval(timerId);doBasicAlert();}
      },1000);
    }
    function updateBasic(){timerBox.textContent=`${remaining}s`;timerBox.classList.toggle("running",remaining<DEFAULT_REST_SEC);}
    function doBasicAlert(){
      const how=alertSel.value;
      if(how==="sound")   new Audio("data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQAAAAA=").play();
      if(how==="vibrate") navigator.vibrate?.([200,100,200]);
      timerBox.classList.remove("running");
    }

    /* ---------- MÁQUINAS cargadas ---------- */
    const machines  = await (await fetch(`${API_BASE}/api/machines`,{headers:authHeaders()})).json();
    const container = qs("#daily-machines"); container.innerHTML="";
    machines.forEach(m=>{
      const row=create("div","flex gap-2 machine-row");
      row.innerHTML=
        `<span class="flex-1">${m.machine.name}</span>
         <input type="number" class="input w-24" value="${m.weightKg}" data-id="${m.machine.id}">
         <input type="number" class="input w-16" value="${m.reps}"      data-r="reps">
         <input type="number" class="input w-16" value="${m.sets}"      data-r="sets">`;
      container.appendChild(row);

      /* inicia temporizador básico al cambiar reps */
      row.querySelector("[data-r='reps']").addEventListener("change",startBasicTimer);
    });

    /* ---------- guardar registro manual ---------- */
    qs("#daily-form").onsubmit = async e=>{
      e.preventDefault();
      const ex=[];
      container.querySelectorAll(".machine-row").forEach(r=>{
        ex.push({
          name:r.querySelector("span").textContent.trim(),
          weightKg:+r.querySelector("[data-id]").value,
          reps:+r.querySelector("[data-r='reps']").value,
          sets:+r.querySelector("[data-r='sets']").value
        });
      });
      await fetch(`${API_BASE}/api/daily`,{
        method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},
        body:JSON.stringify({date:dateInput.value,exercises:ex})
      });
      alert("Registro guardado");
    };

    /* ---------- QUICK-ADD (botón flotante) ---------- */
    qs("#quick-add-btn").onclick = async()=>{
      const today=new Date().toISOString().slice(0,10);
      const ex=machines.map(m=>({name:m.machine.name,weightKg:m.weightKg,reps:m.reps,sets:m.sets}));
      await fetch(`${API_BASE}/api/daily`,{
        method:"POST",headers:{...authHeaders(),"Content-Type":"application/json"},
        body:JSON.stringify({date:today,exercises:ex})
      });
      alert("Registro rápido guardado ✔️");
    };

    /* ==================================================================
       === ADVANCED TIMER  (cronómetro / cuenta-atrás / alarma)  =========
       ================================================================== */
    const modeSelAdv = qs("#timer-mode");
    if(!modeSelAdv) return;                  // plantilla antigua?

    const displayAdv = qs("#series-timer"),
          timeInput  = qs("#timer-input"),
          presetList = qs("#preset-list"),
          presetStar = qs("#add-preset"),
          startAdv   = qs("#timer-start"),
          pauseAdv   = qs("#timer-pause"),
          resetAdv   = qs("#timer-reset");

    /* ---- presets ---- */
    const PRESET_KEY="gym_presets_v1";
    const presets=JSON.parse(localStorage.getItem(PRESET_KEY)||"[]");
    renderPresets();
    presetStar.onclick = ()=>{presets.push(timeInput.value);localStorage.setItem(PRESET_KEY,JSON.stringify(presets));renderPresets();};
    presetList.onchange = ()=>{if(presetList.value) timeInput.value=presetList.value;};

    modeSelAdv.onchange = ()=>qsa(".countdown-field").forEach(el=>el.style.display=modeSelAdv.value==="stopwatch"?"none":"inline-flex");
    modeSelAdv.onchange();  // inicial

    /* ---- lógica advanced ---- */
    let advId=null, advRem=0, advStart=null;

    startAdv.onclick = ()=>{if(!advId) startAdvanced();};
    pauseAdv.onclick = ()=>{clearInterval(advId);advId=null;};
    resetAdv.onclick = ()=>{clearInterval(advId);advId=null;updateAdv(0);};

    function startAdvanced(){
      clearInterval(advId);
      if(modeSelAdv.value==="stopwatch"){
        advStart=Date.now();
        advId=setInterval(()=>updateAdv(Date.now()-advStart),1000);
      }else{
        advRem = modeSelAdv.value==="countdown" ? hmsToMs(timeInput.value) : msUntilAlarm(timeInput.value);
        updateAdv(advRem);
        advId=setInterval(()=>{
          advRem-=1000;updateAdv(advRem);
          if(advRem<=0){clearInterval(advId);advId=null;triggerAlert();}
        },1000);
      }
    }
    function updateAdv(ms){
      if(ms<0) ms=0;
      const s=Math.floor(ms/1000)%60,m=Math.floor(ms/60000)%60,h=Math.floor(ms/3600000);
      displayAdv.textContent=[h,m,s].map(v=>String(v).padStart(2,"0")).join(":");
      displayAdv.classList.toggle("running",!!advId);
    }
    const hmsToMs = t=>{const[a,b,c]=t.split(":").map(Number);return((a*60+b)*60+c)*1000;};
    const msUntilAlarm = t=>{
      const now=new Date(); const[h,m,s]=t.split(":").map(Number);
      const target=new Date(now); target.setHours(h,m,s,0);
      if(target<=now) target.setDate(target.getDate()+1);
      return target-now;
    };
    function triggerAlert(){
      const how=alertSel.value;
      if(how==="sound")   new Audio("data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQAAAAA=").play();
      if(how==="vibrate") navigator.vibrate?.([400,100,400]);
    }
    function renderPresets(){
      presetList.innerHTML="<option value=''>Presets</option>"+presets.map(p=>`<option>${p}</option>`).join("");
    }
  }/* ← fin dailyView */

  /* ------------------------------------------------------------------
     REPORTS
     ------------------------------------------------------------------ */
  function reportsView(){
    const fullBtn=qs("#full-pdf"), rangeBtn=qs("#range-pdf");
    fullBtn.onclick = ()=>download(`${API_BASE}/api/report/full`,"progreso.pdf");
    rangeBtn.onclick=()=>{
      const f=qs("#from").value,t=qs("#to").value;
      if(!f||!t) return alert("Seleccione ambas fechas");
      download(`${API_BASE}/api/report/period?from=${f}&to=${t}`,`progreso_${f}_${t}.pdf`);
    };
  }
  async function download(url,filename){
    const res=await fetch(url,{headers:authHeaders()});
    if(!res.ok) return alert(`Error ${res.status}`);
    const blob=await res.blob(), href=URL.createObjectURL(blob),
          a=create("a"); a.href=href; a.download=filename; a.style.display="none";
    document.body.appendChild(a); a.click();
    setTimeout(()=>{URL.revokeObjectURL(href); a.remove();},800);
  }

  /* ------------------------------------------------------------------
     LOGOUT
     ------------------------------------------------------------------ */
  qs("#logout").onclick = ()=>{
    localStorage.removeItem(TOKEN
