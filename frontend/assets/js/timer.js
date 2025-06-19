/* =========================================================================
   ADVANCED TIMER – v2
   – Cronómetro & Cuenta-atrás con alerta “Sonido / Vibrar / Ambos”.
   – Mantiene vibración hasta que el usuario pulsa la pantalla.
   – Audio YouTube en modo “solo sonido” oculto (búfer interno HTML5 de respaldo).
   – Wake-Lock para seguir sonando/vibrando con pantalla apagada (Android ≥ 12).
   ========================================================================= */

import { qs, create } from "./utils.js";

export function initAdvancedTimer(parent) {
  /* ─── evita múltiples instancias ─── */
  if (qs("#timer-box")) return;

  /* ---------- HTML UI ---------- */
  parent.prepend(createTimerBox());

  /* ---------- refs ---------- */
  const display  = qs("#series-timer");
  const modeSel  = qs("#timer-mode");
  const hField   = qs("#h-field");
  const mField   = qs("#m-field");
  const sField   = qs("#s-field");
  const alertSel = qs("#alert-mode");
  const playBtn  = qs("#timer-start");
  const pauseBtn = qs("#timer-pause");
  const resetBtn = qs("#timer-reset");

  /* ---------- estado ---------- */
  let intervalId = null;
  let base       = 0;          // start -o- final según modo
  let running    = false;
  let vibLoopId  = null;
  let wakeLock   = null;

  /* ---------- YouTube “audio-only” ---------- */
  let ytReady = false, ytPlayer;
  ensureYT();

  /* ---------- eventos ---------- */
  playBtn.onclick  = start;
  pauseBtn.onclick = pause;
  resetBtn.onclick = reset;

  ["pointerdown","touchstart","keydown","visibilitychange"]
    .forEach(ev => window.addEventListener(ev, stopAlerts, { passive:true }));

  /* ============ funciones ============ */

  function start() {
    if (running) return;
    stopAlerts();
    running = true; toggleButtons();

    if (modeSel.value === "stopwatch") {
      base = Date.now() - (parseInt(display.dataset.ms || 0));
      intervalId = setInterval(() => update(Date.now() - base), 1000);
    } else {                               // countdown
      const total = (+hField.value * 3600 + +mField.value * 60 + +sField.value) * 1000;
      if (total <= 0) { alert("Define un tiempo"); reset(); return; }
      base = Date.now() + total;
      intervalId = setInterval(() => {
        const left = base - Date.now();
        update(left);
        if (left <= 0) { pause(); alertNow(); }
      }, 1000);
      update(total);
    }
    getWakeLock();
  }

  function pause() {
    clearInterval(intervalId); intervalId = null;
    running = false; toggleButtons();
    display.dataset.ms = modeSel.value === "stopwatch"
        ? Date.now() - base            // elapsed
        : base - Date.now();           // remaining
    releaseWakeLock();
  }

  function reset() {
    pause();
    display.dataset.ms = 0;
    update(0);
  }

  function update(ms) {
    if (ms < 0) ms = 0;
    const s = Math.floor(ms / 1000) % 60;
    const m = Math.floor(ms / 60000) % 60;
    const h = Math.floor(ms / 3600000);
    display.textContent = [h,m,s].map(v => String(v).padStart(2,"0")).join(":");
    display.classList.toggle("running", running);
  }

  function toggleButtons() {
    playBtn.classList.toggle("hidden",  running);
    pauseBtn.classList.toggle("hidden", !running);
  }

  /* ---------- alertas ---------- */
  function alertNow() {
    const mode = alertSel.value;
    if (mode === "vibrate" || mode === "soundvib") startVib();
    if (mode === "sound"   || mode === "soundvib") playAudio();
  }

  function startVib() {
    if (!navigator.vibrate) return;
    navigator.vibrate(10000);
    vibLoopId = setInterval(() => navigator.vibrate(10000), 10000);
  }

  function stopAlerts() {
    clearInterval(vibLoopId); vibLoopId = null;
    navigator.vibrate?.(0);
    if (ytReady && ytPlayer?.stopVideo) ytPlayer.stopVideo();
  }

  /* ---------- wake-lock ---------- */
  async function getWakeLock() {
    try {
      if ('wakeLock' in navigator && !wakeLock) {
        wakeLock = await navigator.wakeLock.request('screen');
        wakeLock.addEventListener('release', () => (wakeLock = null));
      }
    } catch { /* silencioso */ }
  }
  function releaseWakeLock() { wakeLock?.release(); wakeLock = null; }

  /* ---------- YouTube invisible ---------- */
  function ensureYT() {
    if (window.YT?.Player) return onYT();
    window.onYouTubeIframeAPIReady = onYT;
  }
  function onYT() {
    if (ytPlayer) return;
    const div = create("div"); div.id = "yt-audio";
    div.style.cssText = "position:absolute;width:0;height:0;overflow:hidden;";
    document.body.appendChild(div);
    ytPlayer = new YT.Player("yt-audio", {
      videoId: "JoolQUDWq-k",
      playerVars:{ controls:0, modestbranding:1 },
      events:{ onReady: () => (ytReady = true) }
    });
  }
  function playAudio() {
    if (!ytReady) { setTimeout(playAudio, 400); return; }
    ytPlayer.seekTo(0);
    ytPlayer.playVideo();
  }

  /* ---------- helper ---------- */
  function createTimerBox() {
    const box = document.createElement("div");
    box.id = "timer-box";
    box.className = "timer-box fade-in";
    box.innerHTML = /*html*/`
      <span id="series-timer" class="timer">00:00:00</span>

      <select id="timer-mode" class="input w-32">
        <option value="stopwatch">Cronómetro</option>
        <option value="countdown">Cuenta atrás</option>
      </select>

      <input id="h-field" type="number" class="input w-14" min="0" placeholder="h">
      <input id="m-field" type="number" class="input w-14" min="0" max="59" placeholder="m">
      <input id="s-field" type="number" class="input w-14" min="0" max="59" placeholder="s">

      <select id="alert-mode" class="input w-28">
        <option value="soundvib">Sonido + Vibrar</option>
        <option value="sound">Sólo sonido</option>
        <option value="vibrate">Sólo vibrar</option>
      </select>

      <button id="timer-start" class="btn-icon" title="Play">▶</button>
      <button id="timer-pause" class="btn-icon hidden" title="Pause">⏸</button>
      <button id="timer-reset" class="btn-icon" title="Reset">⭯</button>
    `;
    return box;
  }
}
