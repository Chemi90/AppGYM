/* =========================================================================
   VISTA RPG  –  Versión conectada al backend
   -------------------------------------------------------------------------
   • Obtiene la ficha real con GET /api/rpg/character
   • Chat → POST /api/rpg/chat
   • Foto ejercicio → POST /api/rpg/exercise  (actualiza barras + XP)
   • Foto comida    → POST /api/rpg/meal      (solo análisis por ahora)
   ========================================================================= */

import { api }           from "../api.js";
import { qs, create }    from "../utils.js";

/* ------------------------------- MAIN ---------------------------------- */
export async function loadRpg(container) {

  container.innerHTML = /*html*/`
    <section class="rpg-view fade-in">
      <h2 class="view-title">Aventura RPG</h2>

      <!-- Ficha personaje -->
      <div id="rpg-sheet" class="grid gap-6 md:grid-cols-2 mb-6"></div>

      <!-- Chat + botones -->
      <div class="flex gap-2 items-start mb-4">
        <input  id="chat-input" class="input flex-1" placeholder="Habla con tu maestro…" />
        <button id="chat-send"  class="btn">Enviar</button>

        <!-- selector archivo oculto + botón -->
        <input  id="photo-input" type="file" accept="image/*" hidden />
        <button id="photo-btn" class="btn-icon" title="Subir foto">📷</button>
      </div>

      <!-- Conversación -->
      <div id="chat-box"
           class="space-y-2 bg-gray-900 text-gray-100 p-4 rounded-md
                  h-64 overflow-auto text-sm"></div>
    </section>
  `;

  /* -------- referencias DOM -------- */
  const sheetEl = qs("#rpg-sheet",  container);
  const chatBox = qs("#chat-box",   container);
  const chatIn  = qs("#chat-input", container);
  const sendBt  = qs("#chat-send",  container);
  const photoBt = qs("#photo-btn",  container);
  const photoIn = qs("#photo-input",container);

  /* -------- carga ficha inicial -------- */
  let char = null;
  try { char = await api.get("/api/rpg/character"); } catch {/* primera vez */ }
  if (char) renderSheet(char);

  /* ===================== CHAT ===================== */
  sendBt.onclick = sendChat;
  chatIn.addEventListener("keydown", e => { if (e.key === "Enter") sendChat(); });

  async function sendChat() {
    const txt = chatIn.value.trim();
    if (!txt) return;
    append("user", txt);
    chatIn.value = "";

    const { reply } = await api.post("/api/rpg/chat", { prompt: txt });
    append("ai", reply);
  }

  /* ===================== FOTO ===================== */
  photoBt.onclick = () => photoIn.click();
  photoIn.onchange = async () => {
    const file = photoIn.files[0];
    if (!file) return;

    const base64 = await toBase64(file);
    const exercise = confirm("Aceptar = Ejercicio · Cancelar = Comida");

    if (exercise) {
      /* envía y recibe ficha actualizada */
      const pj = await api.post("/api/rpg/exercise", { imageBase64: base64 });
      renderSheet(pj);
      append("ai", `🏋️ ¡Ganaste ${pj.str} de Fuerza!  XP: ${pj.xp}/${pj.xpNext}`);
    } else {
      const { analysis } = await api.post("/api/rpg/meal", { imageBase64: base64 });
      append("ai", `🍽️ ${analysis}`);
    }
    photoIn.value = "";      // limpia input
  };

  /* ==================== HELPERS =================== */

  function renderSheet(pj) {
    sheetEl.innerHTML = /*html*/`
      <article class="card shadow">
        <header class="card-header">
          <h3>${pj.clazz} · Nivel ${pj.level}</h3>
        </header>
        <div class="card-body grid gap-2">
          <p><strong>XP:</strong> ${pj.xp} / ${pj.xpNext}</p>
          ${bar("Fuerza",    pj.str)}
          ${bar("Energía",   pj.eng)}
          ${bar("Vitalidad", pj.vit)}
          ${bar("Sabiduría", pj.wis)}
        </div>
      </article>
    `;
  }

  const bar = (label, val) =>
    `<p>${label} <progress value="${val}" max="999"></progress> <b>${val}</b></p>`;

  const append = (role, txt) => {
    const p = create("p", role === "ai" ? "sys" : "user");
    p.textContent = txt;
    chatBox.appendChild(p);
    chatBox.scrollTop = chatBox.scrollHeight;
  };

  const toBase64 = file =>
    new Promise(res => {
      const fr = new FileReader();
      fr.onload = e => res(e.target.result.split(",")[1]);
      fr.readAsDataURL(file);
    });
}
