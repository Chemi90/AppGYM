/* =========================================================================
   GYM TRACKER · Front-end (Netlify) · 2025-06
   Quick-Add  ·  Timer básico  ·  Cronómetro / Countdown / Alarma (solo audio)
   ========================================================================= */

/* ------------------------------------------------------------------ CONFIG */
const API_BASE  = import.meta?.env?.VITE_API_BASE
               || "https://appgym-production-64ac.up.railway.app";
const TOKEN_KEY = "gym_token";
const DEFAULT_REST_SEC = 90;      // temporizador básico (seg)

/* ---------------------------------------------------------------- HELPERS */
const qs  = (sel, el=document) => el.querySelector(sel);
const qsa = (sel, el=document) => el.querySelectorAll(sel);
const create = (t,c="") => { const e=document.createElement(t); if(c) e.className=c; return e; };
const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` });

/* ---------------------------------------------------------------- ROUTING */
if (location.pathname.endsWith("/index.html") || location.pathname === "/") {
  authPage();
} else {
  dashboard();
}

/* ========================================================================
   1) AUTH PAGE
   ======================================================================== */
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
    const body = { email: form.email.value, password: form.password.value };
    if (mode === "register") body.confirm = form.confirm.value;

    const res = await fetch(`${API_BASE}/api/auth/${mode}`, {
      method : "POST",
      headers: { "Content-Type":"application/json" },
      body   : JSON.stringify(body)
    });
    if (!res.ok) return alert(await res.text());

    const { token } = await res.json();
    localStorage.setItem(TOKEN_KEY, token);
    location.href = "dashboard.html";
  };
}

/* ========================================================================
   2) DASHBOARD
   ======================================================================== */
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

  /* ---------------- render según hash ---------------- */
  async function render() {
    const view = location.hash.slice(1) || "profile";
    container.innerHTML = "";
    const frag = templates[view].content.cloneNode(true);
    frag.firstElementChild?.classList.add("fade-in");
    container.appendChild(frag);

    if (view === "profile")  profileView();
    if (view === "stats")    statsView();
    if (view === "machines") machinesView();
    if (view === "daily")    dailyView();
    if (view === "reports")  reportsView();
  }

  /* ------------------------------------------------------------------
     PROFILE
     ------------------------------------------------------------------ */
  async function profileView() {
    const res  = await fetch(`${API_BASE}/api/profile`, { headers: authHeaders() });
    const data = await res.json();
    const form = qs("#profile-form");

    Object.entries({
      firstName:data.firstName, lastName:data.lastName, age:data.age,
      height:data.heightCm,     weight:data.weightKg
    }).forEach(([id,val]) => { if (val!=null) form[id].value = val; });

    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        firstName:form.firstName.value, lastName:form.lastName.value,
        age:+form.age.value, heightCm:+form.height.value, weightKg:+form.weight.value
      };
      await fetch(`${API_BASE}/api/profile`, {
        method:"PUT", headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify(body)
      });
      alert("Perfil actualizado");
    };
  }

  /* ------------------------------------------------------------------
     STATS
     ------------------------------------------------------------------ */
  async function statsView() {
    const form = qs("#stats-form");
    form["stats-date"].value = new Date().toISOString().slice(0,10);

    form.onsubmit = async e => {
      e.preventDefault();
      const nf = v => v==="" ? null : +v;
      const body = {
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
      await fetch(`${API_BASE}/api/stats`, {
        method:"POST", headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify(body)
      });
      alert("Medidas guardadas");
      form.reset();
    };
  }

  /* ------------------------------------------------------------------
     MACHINES  (edición inline en la misma fila)
     ------------------------------------------------------------------ */
  async function machinesView() {
    const table = qs("#machine-table");
    const list  = await (await fetch(`${API_BASE}/api/machines`, { headers:authHeaders() })).json();
    renderRows(list);

    const form = qs("#machine-form");
    form.onsubmit = async e => {
      e.preventDefault();
      const body = {
        name:form["machine-name"].value,
        weightKg:+form["machine-kg"].value,
        reps:+form["machine-reps"].value,
        sets:+form["machine-sets"].value
      };
      await fetch(`${API_BASE}/api/machines`, {
        method:"POST", headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify(body)
      });
      form.reset();
      renderRows(await (await fetch(`${API_BASE}/api/machines`,{headers:authHeaders()})).json());
    };

    function renderRows(rows) {
      table.innerHTML="";
      rows.forEach(m=>{
        const tr=create("tr");
        tr.innerHTML =
          `<td class="machine-name">${m.machine.name}</td>
           <td class="machine-kg">${m.weightKg}</td>
           <td class="machine-reps">${m.reps}</td>
           <td class="machine-sets">${m.sets}</td>
           <td class="text-right">
              <button class="btn-icon btn-edit" title="Editar" data-id="${m.id}">✎</button>
              <button class="btn-danger"       title="Eliminar" data-id="${m.id}">×</button>
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
      const kgTd=tr.querySelector(".machine-kg"),
            rTd=tr.querySelector(".machine-reps"),
            sTd=tr.querySelector(".machine-sets");
      kgTd.innerHTML=`<input type="number" class="input w-24" value="${m.weightKg}">`;
      rTd.innerHTML =`<input type="number" class="input w-16" value="${m.reps}">`;
      sTd.innerHTML =`<input type="number" class="input w-16" value="${m.sets}">`;
      const td=tr.lastElementChild;td.querySelector(".btn-edit").remove();
      const save=create("button","btn"); save.textContent="Guardar"; td.prepend(save);
      save.onclick=()=>saveEdit(tr,m);
    }
    async function saveEdit(tr,m){
      const kg  =+tr.querySelector(".machine-kg input").value,
            reps=+tr.querySelector(".machine-reps input").value,
            sets=+tr.querySelector(".machine-sets input").value;
      await fetch(`${API_BASE}/api/machines`,{
        method:"POST",headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify({name:m.machine.name,weightKg:kg,reps,sets})
      });
      tr.querySelector(".machine-kg").textContent=kg;
      tr.querySelector(".machine-reps").textContent=reps;
      tr.querySelector(".machine-sets").textContent=sets;
      const td=tr.lastElementChild;td.querySelector("button.btn").remove();
      const edit=create("button","btn-icon btn-edit"); edit.textContent="✎"; edit.title="Editar";
      edit.onclick=()=>startEdit(tr,m); td.prepend(edit);
    }
  }

  /* ------------------------------------------------------------------
     DAILY  (Quick-Add + Timer básico + Cronómetro / Countdown / Alarma)
     ------------------------------------------------------------------ */
  async function dailyView() {
    const dateIn = qs("#entry-date");
    dateIn.value = new Date().toISOString().slice(0,10);

    /* --- TIMER BÁSICO entre series ----------------------------------- */
    const timerBox = qs("#series-timer");
    const alertSelBasic = qs("#alert-mode");
    let basicId=null, remaining=DEFAULT_REST_SEC;
    function startBasic(){clearInterval(basicId);remaining=DEFAULT_REST_SEC;updBasic();
      basicId=setInterval(()=>{remaining--;updBasic();if(remaining<=0){clearInterval(basicId);alertBasic();}},1000);}
    function updBasic(){timerBox.textContent=`${remaining}s`;timerBox.classList.toggle("running",remaining<DEFAULT_REST_SEC);}
    function alertBasic(){if(alertSelBasic.value==="sound")playYouTube();if(alertSelBasic.value==="vibrate")startVibration();}

    /* --- Máquina list ------------------------------------------------- */
    const machines  = await (await fetch(`${API_BASE}/api/machines`,{headers:authHeaders()})).json();
    const cont = qs("#daily-machines"); cont.innerHTML="";
    machines.forEach(m=>{
      const row=create("div","flex gap-2 machine-row");
      row.innerHTML=
        `<span class="flex-1">${m.machine.name}</span>
         <input type="number" class="input w-24" value="${m.weightKg}" data-id="${m.machine.id}">
         <input type="number" class="input w-16" value="${m.reps}"      data-r="reps">
         <input type="number" class="input w-16" value="${m.sets}"      data-r="sets">`;
      cont.appendChild(row);
      row.querySelector("[data-r='reps']").addEventListener("change",startBasic);
    });

    qs("#daily-form").onsubmit = async e=>{
      e.preventDefault();
      const exercises=[];
      cont.querySelectorAll(".machine-row").forEach(r=>{
        exercises.push({
          name:r.querySelector("span").textContent.trim(),
          weightKg:+r.querySelector("[data-id]").value,
          reps:+r.querySelector("[data-r='reps']").value,
          sets:+r.querySelector("[data-r='sets']").value
        });
      });
      await fetch(`${API_BASE}/api/daily`,{
        method:"POST", headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify({date:dateIn.value,exercises})
      });
      alert("Registro guardado");
    };

    /* --- QUICK-ADD ---------------------------------------------------- */
    qs("#quick-add-btn").onclick = async()=>{
      const today=new Date().toISOString().slice(0,10);
      const ex=machines.map(m=>({name:m.machine.name,weightKg:m.weightKg,reps:m.reps,sets:m.sets}));
      await fetch(`${API_BASE}/api/daily`,{
        method:"POST",headers:{ ...authHeaders(), "Content-Type":"application/json" },
        body:JSON.stringify({date:today,exercises:ex})
      });
      alert("Registro rápido guardado ✔️");
    };

    /* ===================================================================
       === ADVANCED TIMER (cronómetro / cuenta-atrás / alarma) ===========
       =================================================================== */
    const modeSel   = qs("#timer-mode");
    if (!modeSel) return;                          // plantillas antiguas

    const display   = qs("#series-timer");
    const timeInput = qs("#timer-input");
    const presetSel = qs("#preset-list");
    const starBtn   = qs("#add-preset");
    const playBtn   = qs("#timer-start");
    const pauseBtn  = qs("#timer-pause");
    const resetBtn  = qs("#timer-reset");
    const alertSel  = qs("#alert-mode");

    /* ---- presets ---- */
    const KEY="gym_presets_v1";
    const presets=JSON.parse(localStorage.getItem(KEY)||"[]");
    renderPresetSelect();
    starBtn.onclick=()=>{presets.push(timeInput.value);localStorage.setItem(KEY,JSON.stringify(presets));renderPresetSelect();};
    presetSel.onchange=()=>{if(presetSel.value)timeInput.value=presetSel.value;};

    modeSel.onchange=()=>qsa(".countdown-field").forEach(el=>el.style.display=modeSel.value==="stopwatch"?"none":"inline-flex");
    modeSel.onchange();

    /* ---- estado timer avanzado ---- */
    let advId=null, advStart=null, advRem=0, vib=false, vibLoop=null;
    let ytReady=false, ytPlayer=null;
    loadYTApi();

    playBtn.onclick = ()=>{ if(!advId) startAdv(); };
    pauseBtn.onclick=()=>{ clearInterval(advId); advId=null; };
    resetBtn.onclick=()=>{ clearInterval(advId); advId=null; updateAdv(0); stopAlerts(); };

    ["touchstart","pointerdown","keydown"].forEach(ev=>window.addEventListener(ev,stopAlerts,{passive:true}));

    /* ---- funciones timer avanzado ---- */
    function startAdv(){
      stopAlerts(); clearInterval(advId);
      if(modeSel.value==="stopwatch"){
        advStart=Date.now();
        advId=setInterval(()=>updateAdv(Date.now()-advStart),1000);
      }else{
        advRem = modeSel.value==="countdown" ? hmsToMs(timeInput.value) : msUntilAlarm(timeInput.value);
        updateAdv(advRem);
        advId=setInterval(()=>{
          advRem-=1000;updateAdv(advRem);
          if(advRem<=0){clearInterval(advId);advId=null;triggerAlert();}
        },1000);
      }
    }
    function updateAdv(ms){
      if(ms<0)ms=0;
      const s=Math.floor(ms/1000)%60,m=Math.floor(ms/60000)%60,h=Math.floor(ms/3600000);
      display.textContent=[h,m,s].map(v=>String(v).padStart(2,"0")).join(":");
      display.classList.toggle("running",!!advId);
    }
    const hmsToMs=t=>{const[a,b,c]=t.split(":").map(Number);return((a*60+b)*60+c)*1000;};
    const msUntilAlarm=t=>{
      const [h,m,s]=t.split(":").map(Number);
      const now=new Date();const tgt=new Date(now);tgt.setHours(h,m,s,0);if(tgt<=now)tgt.setDate(tgt.getDate()+1);
      return tgt-now;
    };

    /* ---- alertas ---- */
    function triggerAlert(){
      const mode=alertSel.value;
      if(mode==="sound")playYouTube();
      if(mode==="vibrate")startVibration();
      if(mode==="soundvib" || mode==="vibrateSound"){playYouTube();startVibration();}
    }
    function startVibration(){
      if(!navigator.vibrate)return;
      vib=true;navigator.vibrate(10000);
      vibLoop=setInterval(()=>{if(vib)navigator.vibrate(10000);},10000);
    }
    function stopAlerts(){
      vib=false;clearInterval(vibLoop);navigator.vibrate?.(0);
      if(ytPlayer&&ytReady)ytPlayer.stopVideo();
    }

    /* ---- YouTube “solo audio” ---- */
    function loadYTApi(){
      if(window.YT){onYTReady();return;}
      const s=document.createElement("script");s.src="https://www.youtube.com/iframe_api";
      document.head.appendChild(s);
      window.onYouTubeIframeAPIReady=onYTReady;
    }
    function onYTReady(){
      if(ytPlayer)return;
      const d=create("div");d.id="yt-audio";d.style.cssText="position:absolute;left:-9999px;width:0;height:0;overflow:hidden;";
      document.body.appendChild(d);
      ytPlayer=new YT.Player("yt-audio",{
        height:"0",width:"0",videoId:"JoolQUDWq-k",
        playerVars:{autoplay:0,controls:0,fs:0,rel:0,playsinline:1,modestbranding:1},
        events:{onReady:()=>{ytReady=true;}}
      });
    }
    function playYouTube(){
      if(!ytReady){setTimeout(playYouTube,500);return;}
      ytPlayer.seekTo(0); ytPlayer.playVideo();
    }
    function renderPresetSelect(){presetSel.innerHTML="<option value=''>Presets</option>"+presets.map(p=>`<option>${p}</option>`).join("");}
  } /* ----- fin dailyView ----- */

  /* ------------------------------------------------------------------
     REPORTS
     ------------------------------------------------------------------ */
  function reportsView() {
    const fullBtn  = qs("#full-pdf");
    const rangeBtn = qs("#range-pdf");

    fullBtn.onclick  = () => download(`${API_BASE}/api/report/full`, "progreso.pdf");
    rangeBtn.onclick = () => {
      const f = qs("#from").value, t = qs("#to").value;
      if (!f || !t) return alert("Seleccione ambas fechas");
      download(`${API_BASE}/api/report/period?from=${f}&to=${t}`, `progreso_${f}_${t}.pdf`);
    };
  }
  async function download(url, file){
    const res=await fetch(url,{headers:authHeaders()});
    if(!res.ok)return alert(`Error ${res.status}`);
    const blob=await res.blob(); const href=URL.createObjectURL(blob);
    const a=create("a");a.href=href;a.download=file;a.style.display="none";
    document.body.appendChild(a);a.click();
    setTimeout(()=>{URL.revokeObjectURL(href);a.remove();},800);
  }

		  /* ------------------------------------------------------------------
     LOGOUT
     ------------------------------------------------------------------ */  qs("#logout").onclick = () => {
    localStorage.removeItem(TOKEN_KEY);
    location.href = "index.html";
  };
}