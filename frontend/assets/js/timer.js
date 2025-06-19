/* =============================================================================
   TEMPORIZADOR 3-en-1  ·  v3
   – Modos:   stopwatch | countdown        (⚠  alarm eliminado)
   – Controles:  ▶ / ⏸  (toggle)  y  ⭯ reset
   – Audio YouTube oculto  +  Vibración continua
   ========================================================================== */
import { create, qs, qsa } from "./utils.js";

/* ------------------------------------------------------------------ ajustes */
const YT_ID = "JoolQUDWq-k";      // beep / gong
const VOL   = 85;                 // volumen %
const VIB_MS = 10_000;            // vibrar 10 s bucle

/* ------------------------------------------------------------------ estado */
let ytPlayer, ytReady = false;

/* ● API pública: se llama desde views/daily.js -------------------------- */
export function initAdvancedTimer(host) {
  /* evita duplicar si el usuario vuelve a #daily */
  if (qs("#timer-box", host)) return;

  host.innerHTML = markup();                    // inject UI

  /* --------------- refs DOM ---------------- */
  const disp   = qs("#series-timer", host);
  const mode   = qs("#timer-mode", host);
  const hIn    = qs("#h-field", host);
  const mIn    = qs("#m-field", host);
  const sIn    = qs("#s-field", host);
  const alert  = qs("#alert-mode", host);

  const toggle = qs("#timer-toggle", host);     // ▶ / ⏸
  const reset  = qs("#timer-reset",  host);     // ⭯

  /* --------------- visibilidad campos ---------------- */
  mode.onchange = () =>
    qsa(".countdown-field", host).forEach(el =>
      el.style.display = mode.value === "stopwatch" ? "none" : "inline-flex");
  mode.onchange();

  /* --------------- variables runtime ---------------- */
  let running   = false;
  let id        = null;
  let tStart    = 0;          // ms epoch cuando se lanzó
  let elapsed   = 0;          // ms acum. en stopwatch (pausa)
  let remain    = 0;          // ms restantes en countdown
  let vibLoop   = null;
  let vibOn     = false;

  /* --------------- botones ---------------- */
  toggle.onclick = () => running ? pause() : start();
  reset .onclick = resetAll;

  document.addEventListener("series-changed", () => {
    if (mode.value === "stopwatch") { resetAll(); start(); }
    if (mode.value === "countdown") { resetAll(); start(); }
  });

  ["pointerdown","touchstart","keydown"]
    .forEach(ev => window.addEventListener(ev, stopAlerts, { passive:true }));

  /* =========== funciones principales ================= */
  function start() {
    running = true;
    toggle.textContent = "⏸";

    if (mode.value === "stopwatch") {
      tStart = Date.now() - elapsed;
      tick(); id = setInterval(tick, 1000);
    } else {                            /* countdown */
      if (remain === 0) remain = hmsToMs();
      tick(); id = setInterval(() => {
        remain -= 1000; tick();
        if (remain <= 0) { pause(); alertNow(); }
      }, 1000);
    }
  }

  function pause() {
    running = false;
    toggle.textContent = "▶";
    clearInterval(id); id = null;
    if (mode.value === "stopwatch") elapsed = Date.now() - tStart;
  }

  function resetAll() {
    pause();
    elapsed = 0; remain = 0; updateDisplay(0);
    stopAlerts();
  }

  function tick() {
    const ms = mode.value === "stopwatch"
             ? Date.now() - tStart
             : remain;
    updateDisplay(ms);
  }

  function updateDisplay(ms) {
    if (ms < 0) ms = 0;
    const s = Math.floor(ms/1000)%60,
          m = Math.floor(ms/60000)%60,
          h = Math.floor(ms/3600000);
    disp.textContent = [h,m,s].map(v=>String(v).padStart(2,"0")).join(":");
    disp.classList.toggle("running", running);
  }

  /* =========== alertas ============ */
  function alertNow() {
    const mode = alert.value;
    if (mode.includes("sound"))    playAudio();
    if (mode.includes("vibrate"))  vibrateLoop();
  }
  function vibrateLoop() {
    if (!navigator.vibrate) return;
    vibOn = true;
    navigator.vibrate(VIB_MS);
    vibLoop = setInterval(() => { if (vibOn) navigator.vibrate(VIB_MS); }, VIB_MS);
  }
  function stopAlerts() {
    vibOn = false; clearInterval(vibLoop); navigator.vibrate?.(0);
    if (ytReady && ytPlayer) ytPlayer.stopVideo();
  }

  /* =========== YouTube audio-only ============ */
  ensureYT();
  function playAudio() {
    if (!ytReady) { setTimeout(playAudio, 400); return; }
    ytPlayer.seekTo(0); ytPlayer.playVideo();
  }
  function ensureYT() {
    if (window.YT && window.YT.Player) return build();
    window.onYouTubeIframeAPIReady = build;
  }
  function build() {
    if (ytPlayer) return;
    const d = create("div");
    d.id = "yt-audio";
    d.style.cssText="position:absolute;width:0;height:0;overflow:hidden;";
    document.body.appendChild(d);

    ytPlayer = new YT.Player("yt-audio", {
      videoId   : YT_ID,
      playerVars: {
        autoplay:0, controls:0, fs:0, rel:0,
        origin  : location.origin, playsinline:1
      },
      events    : { onReady: () => { ytReady = true; ytPlayer.setVolume(VOL);} }
    });
  }

  /* =========== helpers ============ */
  function hmsToMs() {
    const h = +hIn.value || 0,
          m = +mIn.value || 0,
          s = +sIn.value || 0;
    return ((h*60 + m)*60 + s) * 1000;
  }
}

/* ---------------- markup dinámico ---------------- */
const markup = () => /*html*/`
  <div id="timer-box" class="timer-box">
    <span id="series-timer" class="timer">00:00:00</span>

    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
    </select>

    <input id="h-field" class="input countdown-field" type="number" min="0" placeholder="hh" style="width:4rem">
    <span class="countdown-field">:</span>
    <input id="m-field" class="input countdown-field" type="number" min="0" placeholder="mm" style="width:4rem">
    <span class="countdown-field">:</span>
    <input id="s-field" class="input countdown-field" type="number" min="0" placeholder="ss" style="width:4rem">

    <select id="alert-mode">
      <option value="sound">Sonido</option>
      <option value="vibrate">Vibrar</option>
      <option value="soundvib">Ambos</option>
    </select>

    <button id="timer-toggle" class="btn-icon" title="Play/Pause">▶</button>
    <button id="timer-reset"  class="btn-icon" title="Reset">⭯</button>
  </div>
`;
