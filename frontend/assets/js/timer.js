/* Temporizador avanzado (cronómetro / cuenta-atrás / alarma)
   – Audio YouTube oculto.  – Vibración continua hasta interacción.
------------------------------------------------------------------- */
import { create, qs, qsa } from "./utils.js";

const YT_ID = "JoolQUDWq-k";          // beep
let ytPlayer, ytReady = false;

/* === API pública (llamada desde loadDaily) ========================== */
export function initAdvancedTimer(host) {
  /* evita duplicar si el user vuelve al tab Daily */
  if (qs("#timer-box", host)) return;

  host.innerHTML = markup();                 // UI

  /* ---------------- refs DOM ---------------- */
  const disp   = qs("#series-timer", host);
  const mode   = qs("#timer-mode", host);
  const hIn    = qs("#h-field", host);
  const mIn    = qs("#m-field", host);
  const sIn    = qs("#s-field", host);
  const preset = qs("#preset-list", host);
  const star   = qs("#add-preset", host);
  const alert  = qs("#alert-mode", host);
  const btnGo  = qs("#timer-start", host);
  const btnPau = qs("#timer-pause", host);
  const btnRes = qs("#timer-reset", host);

  /* ---------------- presets ---------------- */
  const KEY = "gym_presets_v2";
  const presets = JSON.parse(localStorage.getItem(KEY) || "[]");
  const paint   = () =>
    preset.innerHTML = "<option value=''>Presets</option>" +
      presets.map(p => `<option>${p}</option>`).join("");
  paint();
  star.onclick   = () => {
    const str = hmsStr();
    if (str !== "00:00:00" && !presets.includes(str)) {
      presets.push(str);
      localStorage.setItem(KEY, JSON.stringify(presets));
      paint();
    }
  };
  preset.onchange = () => {
    if (!preset.value) return;
    const [h, m, s] = preset.value.split(":");
    hIn.value = +h; mIn.value = +m; sIn.value = +s;
  };

  /* ---------------- modo ↔ campos ---------------- */
  mode.onchange = () =>
    qsa(".countdown-field", host)
      .forEach(el => el.style.display = mode.value === "stopwatch" ? "none" : "inline-flex");
  mode.onchange();

  /* ---------------- estado timer ---------------- */
  let id = null, remain = 0, start = 0, vibLoop = null, vib = false;

  btnGo.onclick  = () => { if (!id) run(); };
  btnPau.onclick = () => { clearInterval(id); id = null; };
  btnRes.onclick = () => { clearInterval(id); id = null; upd(0); stopAlerts(); };

  /* reinicia al cambiar reps en la vista Daily */
  document.addEventListener("series-changed",
    () => { if (mode.value !== "stopwatch") run(); });

  ["pointerdown", "touchstart", "keydown"]
    .forEach(ev => window.addEventListener(ev, stopAlerts, { passive:true }));

  /* ---------------- motor ---------------- */
  function run() {
    stopAlerts(); clearInterval(id);
    if (mode.value === "stopwatch") {
      start = Date.now(); upd(0);
      id = setInterval(() => upd(Date.now() - start), 1000);
    } else {
      remain = mode.value === "countdown"
             ? hmsMs(hmsStr())
             : msToNext(hmsStr());
      upd(remain);
      id = setInterval(() => {
        remain -= 1000; upd(remain);
        if (remain <= 0) { clearInterval(id); id = null; fireAlert(); }
      }, 1000);
    }
  }

  function upd(ms) {
    if (ms < 0) ms = 0;
    const s = Math.floor(ms / 1000) % 60;
    const m = Math.floor(ms / 60000) % 60;
    const h = Math.floor(ms / 3600000);
    disp.textContent = [h, m, s].map(v => String(v).padStart(2, "0")).join(":");
    disp.classList.toggle("running", !!id);
  }

  /* ---------------- alertas ---------------- */
  function fireAlert() {
    const m = alert.value;
    if (m.includes("sound")) playAudio();
    if (m.includes("vibrate")) vibLoopStart();
  }
  function vibLoopStart() {
    if (!navigator.vibrate) return;
    vib = true;
    navigator.vibrate(10000);
    vibLoop = setInterval(() => { if (vib) navigator.vibrate(10000); }, 10000);
  }
  function stopAlerts() {
    vib = false; clearInterval(vibLoop); navigator.vibrate?.(0);
    if (ytReady && ytPlayer) ytPlayer.stopVideo();
  }

  /* ---------------- YouTube “audio-only” ---------------- */
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
    const wrap = create("div");
    wrap.id = "yt-audio";
    wrap.style.cssText = "position:absolute;width:0;height:0;overflow:hidden;";
    document.body.appendChild(wrap);

    ytPlayer = new YT.Player("yt-audio", {
      videoId: YT_ID,
      playerVars: { controls:0, fs:0, rel:0, origin: location.origin, playsinline:1 },
      events: { onReady: () => { ytReady = true; ytPlayer.setVolume(85); } }
    });
  }

  /* ---------------- helpers ---------------- */
  function hmsStr() {
    const h = String(+hIn.value || 0).padStart(2, "0");
    const m = String(+mIn.value || 0).padStart(2, "0");
    const s = String(+sIn.value || 0).padStart(2, "0");
    return `${h}:${m}:${s}`;
  }
  const hmsMs = str => {
    const [h=0,m=0,s=0] = str.split(":").map(Number);
    return ((h*60 + m)*60 + s) * 1000;
  };
  const msToNext = str => {
    const [h=0,m=0,s=0] = str.split(":").map(Number);
    const now = new Date();
    const tgt = new Date(now);
    tgt.setHours(h, m, s, 0);
    if (tgt <= now) tgt.setDate(tgt.getDate() + 1);
    return tgt - now;
  };
}

/* ---------------- markup ---------------- */
const markup = () => /*html*/`
  <div id="timer-box" class="timer-box">
    <span id="series-timer" class="timer">00:00:00</span>

    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
      <option value="alarm">Alarma</option>
    </select>

    <input id="h-field" class="input countdown-field" type="number" min="0" style="width:4rem" placeholder="hh">
    <span  class="countdown-field">:</span>
    <input id="m-field" class="input countdown-field" type="number" min="0" style="width:4rem" placeholder="mm">
    <span  class="countdown-field">:</span>
    <input id="s-field" class="input countdown-field" type="number" min="0" style="width:4rem" placeholder="ss">

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
