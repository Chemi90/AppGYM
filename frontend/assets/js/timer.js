/* =========================================================================
   TIMER 2.2 · Cronómetro + Cuenta-atrás  (sonido + vibración en background)
   ========================================================================= */

import { qs, create } from "./utils.js";

/* ----------------------------------------------------------------  CONSTANTES */
const VIB_PATTERN = [300, 300, 300, 300, 300, 300, 300, 300, 300, 300];   // 10 s
let   ctx, osc, gain;                                                     // Web-Audio

/* ----------------------------------------------------------------  API AUDIO */
function startTone() {
  if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
  if (ctx.state === "suspended") ctx.resume();
  osc   = ctx.createOscillator();
  gain  = ctx.createGain();
  gain.gain.value = 0.12;               // volumen bajo
  osc.frequency.value = 880;            // A5
  osc.type = "square";
  osc.connect(gain).connect(ctx.destination);
  osc.start();
}
function stopTone() {
  if (osc) { osc.stop(); osc.disconnect(); gain.disconnect(); }
  osc = gain = null;
}

/* ----------------------------------------------------------------  WAKE-LOCK */
let wakelock = null;
async function keepAwake() {
  try { wakelock = await navigator.wakeLock?.request("screen"); } catch { /* */ }
}
function releaseAwake() { wakelock?.release?.(); wakelock = null; }

/* ----------------------------------------------------------------  UI */
export function initAdvancedTimer(anchor) {
  if (qs("#timer-box")) return;                 // ya creado

  const box = create("div", "timer-box");
  box.id = "timer-box";
  box.innerHTML = `
    <span  id="series-timer" class="timer">00:00:00</span>

    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
    </select>

    <input id="h" type="number" min="0" max="23" value="0"  class="input w-16">
    <span>:</span>
    <input id="m" type="number" min="0" max="59" value="1"  class="input w-16">
    <span>:</span>
    <input id="s" type="number" min="0" max="59" value="0"  class="input w-16">

    <select id="alert-mode">
      <option value="sound">Sonido</option>
      <option value="vibrate">Vibrar</option>
      <option value="soundvib">Ambos</option>
    </select>

    <button id="btn-toggle" class="btn-icon" title="Iniciar">▶</button>
    <button id="btn-reset"  class="btn-icon" title="Reset">⭯</button>
  `;
  anchor.after(box);

  /* ------------- Elementos ------------- */
  const disp   = qs("#series-timer");
  const modeEl = qs("#timer-mode");
  const hIn    = qs("#h"), mIn = qs("#m"), sIn = qs("#s");
  const toggle = qs("#btn-toggle");
  const reset  = qs("#btn-reset");
  const alertEl= qs("#alert-mode");

  /* ------------- Estado ------------- */
  let id = null, start = 0, remain = 0, running = false;

  /* ------------- Botones ------------- */
  toggle.onclick = () => running ? pause() : startTimer();
  reset .onclick = hardReset;

  /* ------------- Funciones ------------- */
  function startTimer() {
    stopAlert(); keepAwake();
    if (modeEl.value === "stopwatch") {
      start = Date.now() - remain;
      id = setInterval(() => update(Date.now() - start), 1000);
    } else {
      remain = (+hIn.value * 3600 + +mIn.value * 60 + +sIn.value) * 1000;
      if (remain <= 0) return;
      update(remain);
      id = setInterval(() => {
        remain -= 1000;
        update(remain);
        if (remain <= 0) { clearInterval(id); id = null; fireAlert(); }
      }, 1000);
    }
    running = true; toggle.textContent = "⏸";
  }

  function pause() {
    clearInterval(id); id = null;
    running = false; toggle.textContent = "▶";
    releaseAwake();
  }

  function hardReset() {
    pause(); remain = 0; update(0); stopAlert();
  }

  function update(ms) {
    if (ms < 0) ms = 0;
    const s = (ms / 1000) | 0;
    const hh = String((s / 3600)    | 0).padStart(2, "0");
    const mm = String((s / 60) % 60 | 0).padStart(2, "0");
    const ss = String(s % 60).padStart(2, "0");
    disp.textContent = `${hh}:${mm}:${ss}`;
    disp.classList.toggle("running", running);
  }

  /* ------------- Alertas ------------- */
  function fireAlert() {
    running = false; toggle.textContent = "▶"; releaseAwake();

    const mode = alertEl.value;
    if (mode.includes("sound"))   startTone();
    if (mode.includes("vibrate")) vibrateLoop();

    showNotif();
  }

  function stopAlert() {
    stopTone();
    navigator.vibrate?.(0);
    navigator.serviceWorker?.getRegistration()
      .then(r => r?.getNotifications({ tag:"gym-timer" })
      .then(list => list.forEach(n => n.close())));
  }

  /* vibración prolongada */
  function vibrateLoop(){
    if (!navigator.vibrate) return;
    navigator.vibrate(VIB_PATTERN);
    const h = setInterval(()=>navigator.vibrate(VIB_PATTERN), 10000);
    window.addEventListener("pointerdown", ()=>{clearInterval(h);navigator.vibrate(0);},
                            { once:true, passive:true });
  }

  /* notificación persistente */
  async function showNotif(){
    if (!("Notification" in window)) return;
    if (Notification.permission === "default") await Notification.requestPermission();
    if (Notification.permission !== "granted") return;
    navigator.serviceWorker.getRegistration().then(reg=>{
      reg?.showNotification("¡Tiempo!",{
        body:"Siguiente serie",
        tag :"gym-timer",
        vibrate: VIB_PATTERN,
        requireInteraction:true
      });
    });
  }

  /* detener alertas con cualquier interacción */
  ["pointerdown","keydown","touchstart"].forEach(ev =>
    window.addEventListener(ev, stopAlert, { passive:true })
  );
}
