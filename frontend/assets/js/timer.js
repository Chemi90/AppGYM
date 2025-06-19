import { qs, qsa, create } from "./utils.js";

export function initAdvancedTimer(container){
  // inyectamos controles solo una vez
  if (qs("#timer-box")) return;

  container.innerHTML = `
    <div id="timer-box" class="timer-box">
      <span id="series-timer" class="timer">00:00:00</span>
      <select id="timer-mode">
        <option value="stopwatch">Cronómetro</option>
        <option value="countdown">Cuenta atrás</option>
        <option value="alarm">Alarma</option>
      </select>
      <input id="timer-input" class="input countdown-field" type="text" placeholder="hh:mm:ss" style="width:7rem">
      <select id="preset-list" class="countdown-field"></select>
      <button id="add-preset" class="btn-icon countdown-field" title="Guardar preset">★</button>
      <select id="alert-mode">
        <option value="sound">Sonido</option>
        <option value="vibrate">Vibrar</option>
        <option value="soundvib">Ambos</option>
      </select>
      <button id="timer-start" class="btn-icon" title="Start">▶</button>
      <button id="timer-pause" class="btn-icon" title="Pause">⏸</button>
      <button id="timer-reset" class="btn-icon" title="Reset">⭯</button>
    </div>
  `;

  /* --- lógica (idéntica a la versión anterior) --- */
  const modeSel   = qs("#timer-mode");
  const display   = qs("#series-timer");
  const timeInput = qs("#timer-input");
  const presetSel = qs("#preset-list");
  const starBtn   = qs("#add-preset");
  const playBtn   = qs("#timer-start");
  const pauseBtn  = qs("#timer-pause");
  const resetBtn  = qs("#timer-reset");
  const alertSel  = qs("#alert-mode");

  const KEY="gym_presets_v1";
  const presets=JSON.parse(localStorage.getItem(KEY)||"[]");
  const renderPresetSelect=()=>{presetSel.innerHTML="<option value=''>Presets</option>"+presets.map(p=>`<option>${p}</option>`).join("");};
  renderPresetSelect();
  starBtn.onclick=()=>{presets.push(timeInput.value);localStorage.setItem(KEY,JSON.stringify(presets));renderPresetSelect();};
  presetSel.onchange=()=>{if(presetSel.value)timeInput.value=presetSel.value;};
  modeSel.onchange=()=>qsa(".countdown-field").forEach(el=>el.style.display=modeSel.value==="stopwatch"?"none":"inline-flex");
  modeSel.onchange();

  let advId=null, advStart=null, advRem=0, vib=false, vibLoop=null;
  let ytReady=false, ytPlayer=null;
  loadYTApi();

  playBtn.onclick = ()=>{ if(!advId) start(); };
  pauseBtn.onclick=()=>{ clearInterval(advId); advId=null; };
  resetBtn.onclick=()=>{ clearInterval(advId); advId=null; update(0); stopAlerts(); };

  ["pointerdown","touchstart","keydown"].forEach(ev=>window.addEventListener(ev,stopAlerts,{passive:true}));

  function start(){
    stopAlerts(); clearInterval(advId);
    if(modeSel.value==="stopwatch"){
      advStart=Date.now();
      advId=setInterval(()=>update(Date.now()-advStart),1000);
    }else{
      advRem = modeSel.value==="countdown" ? hmsToMs(timeInput.value) : msUntilAlarm(timeInput.value);
      update(advRem);
      advId=setInterval(()=>{
        advRem-=1000; update(advRem);
        if(advRem<=0){clearInterval(advId);advId=null;alert();}},1000);
    }
  }
  function update(ms){
    if(ms<0)ms=0;
    const s=Math.floor(ms/1000)%60,m=Math.floor(ms/60000)%60,h=Math.floor(ms/3600000);
    display.textContent=[h,m,s].map(v=>String(v).padStart(2,"0")).join(":");
    display.classList.toggle("running",!!advId);
  }
  const hmsToMs=t=>{const[a,b,c]=t.split(":").map(Number);return((a*60+b)*60+c)*1000;};
  const msUntilAlarm=t=>{
    const [h,m,s]=t.split(":").map(Number);const now=new Date();const tgt=new Date(now);
    tgt.setHours(h,m,s,0);if(tgt<=now)tgt.setDate(tgt.getDate()+1);return tgt-now;};

  function alert(){
    const mode=alertSel.value;
    if(mode.includes("sound"))playYT(); if(mode.includes("vibrate"))vibrate();
  }
  /* -------- vibración prolongada -------- */
  function vibrate(){
    if(!navigator.vibrate)return; vib=true;navigator.vibrate(10000);
    vibLoop=setInterval(()=>{if(vib)navigator.vibrate(10000);},10000);
  }
  function stopAlerts(){
    vib=false;clearInterval(vibLoop);navigator.vibrate?.(0);
    if(ytPlayer&&ytReady)ytPlayer.stopVideo();
  }
  /* -------- YT API "audio only" -------- */
  function loadYTApi(){
    if(window.YT){onYTReady();return;}
    const s=document.createElement("script");s.src="https://www.youtube.com/iframe_api";document.head.appendChild(s);
    window.onYouTubeIframeAPIReady=onYTReady;
  }
  function onYTReady(){
    if(ytPlayer)return;
    const d=create("div");d.id="yt-audio";d.style.cssText="position:absolute;width:0;height:0;overflow:hidden;";document.body.appendChild(d);
    ytPlayer=new YT.Player("yt-audio",{videoId:"JoolQUDWq-k",playerVars:{controls:0,fs:0,rel:0},events:{onReady:()=>ytReady=true}});
  }
  function playYT(){ if(!ytReady){setTimeout(playYT,500);return;} ytPlayer.seekTo(0); ytPlayer.playVideo(); }
}
