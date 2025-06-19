/* -------------------------------------------------------------------------
   ADVANCED TIMER   (cronómetro   |  cuenta-atrás  |  alertas sonido/vibrar)
   – Posicionado ahora en la parte superior del Daily view.
   – Sólo dos acciones visibles: ▶︎ Play   ⏸ Pause   ⭯ Reset
   – Vibración mantenida (hasta pulsar la pantalla) y audio YouTube silenciado.
   – Intenta mantener alerta con pantalla bloqueada usando Wake Lock.
   ---------------------------------------------------------------------- */

import { qs, create } from "./utils.js";

export function initAdvancedTimer(targetParent) {
  /* ─── no duplicar ─── */
  if (qs("#timer-box")) return;

  /* ---------- UI HTML ---------- */
  targetParent.insertAdjacentHTML("afterbegin", /*html*/`
    <div id="timer-box" class="timer-box fade-in">
      <span id="series-timer" class="timer">00:00:00</span>

      <div class="flex gap-2 items-center">
        <select id="timer-mode" class="input w-32">
          <option value="stopwatch">Cronómetro</option>
          <option value="countdown">Cuenta atrás</option>
        </select>

        <input  id="h-field" type="number" class="input w-14" min="0" placeholder="h">
        <input  id="m-field" type="number" class="input w-14" min="0" max="59" placeholder="m">
        <input  id="s-field" type="number" class="input w-14" min="0" max="59" placeholder="s">

        <select id="alert-mode" class="input w-max">
          <option value="soundvib">Sonido + Vibrar</option>
          <option value="sound">Sólo sonido</option>
          <option value="vibrate">Sólo vibrar</option>
        </select>

        <button id="timer-start" class="btn-icon" title="Start">▶</button>
        <button id="timer-pause" class="btn-icon hidden" title="Pause">⏸</button>
        <button id="timer-reset" class="btn-icon" title="Reset">⭯</button>
      </div>
    </div>
  `);

  /* ---------- referencias ---------- */
  const modeSel   = qs("#timer-mode");
  const hField    = qs("#h-field");
  const mField    = qs("#m-field");
  const sField    = qs("#s-field");
  const display   = qs("#series-timer");
  const playBtn   = qs("#timer-start");
  const pauseBtn  = qs("#timer-pause");
  const resetBtn  = qs("#timer-reset");
  const alertSel  = qs("#alert-mode");

  /* ---------- estado ---------- */
  let tickId     = null;         // setInterval id
  let endTime    = 0;            // timestamp (countdown) o start (cronómetro)
  let isRunning  = false;
  let vibLoopId  = null;
  let wakeLock   = null;

  /* ---------- YouTube audio ---------- */
  let ytReady = false, ytPlayer;
  ensureYT();

  /* ---------- eventos ---------- */
  playBtn.onclick  = () => { if (!isRunning) start(); };
  pauseBtn.onclick = () => pause();
  resetBtn.onclick = () => reset();

  ["pointerdown", "touchstart", "keydown", "visibilitychange"].forEach(ev =>
    window.addEventListener(ev, stopAlerts, { passive: true })
  );

  /* =============== funciones internas =============== */

  function start() {
    stopAlerts();
    isRunning = true;
    toggleButtons();

    if (modeSel.value === "stopwatch") {
      endTime = Date.now() - (parseInt(display.dataset.elapsed || 0));
      tickId  = setInterval(() => update(Date.now() - endTime), 1000);
    } else {                                             // countdown
      const totalMs = (+hField.value * 3600 +
                       +mField.value * 60 +
                       +sField.value) * 1000;
      if (totalMs <= 0) { alert("Define un tiempo"); reset(); return; }

      endTime = Date.now() + totalMs;
      update(totalMs);
      tickId  = setInterval(() => {
        const msLeft = endTime - Date.now();
        update(msLeft);
        if (msLeft <= 0) { pause(); fireAlert(); }
      }, 1000);
    }
    requestWakeLock();
  }

  function pause() {
    clearInterval(tickId); tickId = null;
    isRunning = false;
    display.dataset.elapsed = modeSel.value === "stopwatch"
        ? (Date.now() - endTime) : (endTime - Date.now());
    toggleButtons();
    releaseWakeLock();
  }

  function reset() {
    pause();
    update(0);
    display.dataset.elapsed = 0;
  }

  function update(ms) {
    if (ms < 0) ms = 0;
    const sec = Math.floor(ms / 1000) % 60;
    const min = Math.floor(ms / 60000) % 60;
    const hrs = Math.floor(ms / 3600000);
    display.textContent = [hrs, min, sec].map(v => String(v).padStart(2, "0")).join(":");
    display.classList.toggle("running", isRunning);
  }

  function toggleButtons() {
    playBtn.classList.toggle("hidden",  isRunning);
    pauseBtn.classList.toggle("hidden", !isRunning);
  }

  /* ---------- alertas ---------- */
  function fireAlert() {
    const mode = alertSel.value;
    if (mode.includes("sound"))   playAudio();
    if (mode.includes("vibrate")) startVibrate();
  }

  function startVibrate() {
    if (!navigator.vibrate) return;
    navigator.vibrate(10000);                   // primer ciclo 10 s
    vibLoopId = setInterval(() => navigator.vibrate(10000), 10000);
  }

  function stopAlerts() {
    clearInterval(vibLoopId); vibLoopId = null;
    navigator.vibrate?.(0);
    if (ytReady && ytPlayer.stopVideo) ytPlayer.stopVideo();
  }

  /* ---------- Wake-Lock (mantener CPU/vibración con pantalla apagada) */
  async function requestWakeLock() {
    try {
      if ('wakeLock' in navigator && !wakeLock) {
        wakeLock = await navigator.wakeLock.request('screen');
        wakeLock.addEventListener('release', () => (wakeLock = null));
      }
    } catch (e) { /* puede fallar en iOS / desktop */ }
  }
  function releaseWakeLock() {
    wakeLock?.release(); wakeLock = null;
  }

  /* ---------- audio “oculto” via YouTube sólo-audio ---------- */
  function ensureYT() {
    if (window.YT && window.YT.Player) return onReadyYT();
    // ya cargaste iframe_api en dashboard.html
    window.onYouTubeIframeAPIReady = onReadyYT;
  }

  function onReadyYT() {
    if (ytPlayer) return;
    const wrapper = create("div");
    wrapper.id = "yt-audio";
    wrapper.style.cssText = "position:absolute;width:0;height:0;overflow:hidden;";
    document.body.appendChild(wrapper);

    ytPlayer = new YT.Player("yt-audio", {
      videoId: "JoolQUDWq-k",
      playerVars: { controls: 0, modestbranding: 1 },
      events: { onReady: () => (ytReady = true) }
    });
  }

  function playAudio() {
    if (!ytReady) { setTimeout(playAudio, 400); return; }
    ytPlayer.seekTo(0);
    ytPlayer.playVideo();
  }
}
