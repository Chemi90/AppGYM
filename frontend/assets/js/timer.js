/* =========================================================================
   TIMER MODAL  ▸  Cronómetro + Cuenta atrás
   (v2025-06  ·  Wake-Lock + Notification + Audio offline)
   ========================================================================= */

import { qs, create } from "./utils.js";

/* ───────── CONSTANTES ───────── */
const BEEP =
  "data:audio/wav;base64,UklGRogAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YaAAAAAA////" +
  "AAAAAAAAAAAAAAAAAAD//wAA//8AAP//AAD//wAA//8AAP//AAD//wAA//8AAP//AAD//wAA//8AAP//AAD/"+
  "/wAA";                          // breve beep (≈480 B)

const VIB_PATTERN = [300, 300, 300, 300, 300, 300, 300, 300, 300, 300]; // 10 s

/* ───────── INICIALIZACIÓN ───────── */
export function initAdvancedTimer(anchorEl) {
  if (qs("#timer-box")) return;              // ya existe

  /* ---------- UI ---------- */
  const box = create("div", "timer-box");
  box.id = "timer-box";
  box.innerHTML = `
    <span id="series-timer" class="timer">00:00:00</span>
    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
    </select>
    <input  id="time-seg"   class="input" type="number" min="0" max="59" value="00" style="width:3.5rem">
    <span>:</span>
    <input  id="time-min"   class="input" type="number" min="0" max="59" value="01" style="width:3.5rem">
    <span>:</span>
    <input  id="time-hour"  class="input" type="number" min="0" max="23" value="00" style="width:3.5rem">
    <select id="alert-mode">
      <option value="sound">Sonido</option>
      <option value="vibrate">Vibrar</option>
      <option value="soundvib">Ambos</option>
    </select>
    <button id="btn-play"  class="btn-icon" title="Iniciar">▶</button>
    <button id="btn-reset" class="btn-icon hidden" title="Reset">⭯</button>
  `;
  anchorEl.after(box);                       // siempre arriba del Daily

  /* ---------- VARIABLES ---------- */
  const modeSel = qs("#timer-mode");
  const hIn     = qs("#time-hour");
  const mIn     = qs("#time-min");
  const sIn     = qs("#time-seg");
  const display = qs("#series-timer");
  const playBtn = qs("#btn-play");
  const resetBtn= qs("#btn-reset");
  const alertSel= qs("#alert-mode");

  /* audio en memoria (bucle) */
  const audio   = new Audio(BEEP);
  audio.loop = true;

  /* wake lock */
  let lock = null;
  async function keepAwake() {
    try { lock = await navigator.wakeLock?.request("screen"); }
    catch { /* no-op */ }
  }
  function releaseAwake() { lock?.release?.(); lock = null; }

  /* temporizador */
  let id = null, tStart = 0, remain = 0;

  playBtn.onclick = () => {
    playBtn.classList.add("hidden");
    resetBtn.classList.remove("hidden");
    if (modeSel.value === "stopwatch") {
      tStart = Date.now();
      keepAwake();
      id = setInterval(() => update(Date.now() - tStart), 1000);
    } else {
      /* cuenta atrás */
      remain = (+hIn.value * 3600 + +mIn.value * 60 + +sIn.value) * 1000;
      if (remain <= 0) return reset();
      keepAwake();
      update(remain);
      id = setInterval(() => {
        remain -= 1000;
        update(remain);
        if (remain <= 0) { clearInterval(id); id = null; fireAlert(); }
      }, 1000);
    }
  };

  resetBtn.onclick = reset;

  function reset() {
    clearInterval(id); id = null;
    update(0); stopAlert();
    playBtn.classList.remove("hidden");
    resetBtn.classList.add("hidden");
    releaseAwake();
  }

  function update(ms) {
    if (ms < 0) ms = 0;
    const s = Math.floor(ms / 1000) % 60;
    const m = Math.floor(ms / 60000) % 60;
    const h = Math.floor(ms / 3600000);
    display.textContent = [h, m, s].map(v => String(v).padStart(2, "0")).join(":");
    display.classList.toggle("running", !!id);
  }

  /* ---------- ALERTAS ---------- */
  function fireAlert() {
    const mode = alertSel.value;
    if (mode.includes("sound")) audio.play().catch(()=>{});
    if (mode.includes("vibrate")) vibLoop();
    showNotification();
  }

  /* vibrar 10 s o hasta interacción */
  function vibLoop() {
    if (!navigator.vibrate) return;
    navigator.vibrate(VIB_PATTERN);
    const h = setInterval(() => navigator.vibrate(VIB_PATTERN), 10000);
    window.addEventListener("pointerdown", () => {
      clearInterval(h);
      navigator.vibrate(0);
    }, { once: true, passive: true });
  }

  /* notificación persistente */
  async function showNotification() {
    if (!("Notification" in window)) return;
    if (Notification.permission === "default") await Notification.requestPermission();
    if (Notification.permission !== "granted") return;

    navigator.serviceWorker.getRegistration().then(reg => {
      if (!reg) return;
      reg.showNotification("¡Tiempo!", {
        body: "Continúa con la siguiente serie",
        vibrate: VIB_PATTERN,
        requireInteraction: true,
        tag: "gym-timer"
      });
    });
  }

  function stopAlert() {
    audio.pause(); audio.currentTime = 0;
    navigator.vibrate?.(0);
    navigator.serviceWorker.getRegistration().then(r=>{
      r?.getNotifications({ tag:"gym-timer" }).then(list => list.forEach(n=>n.close()));
    });
  }

  /* detener alertas al primer toque / tecla */
  ["pointerdown","keydown","touchstart"].forEach(ev=>{
    window.addEventListener(ev, stopAlert, { passive:true });
  });
}
