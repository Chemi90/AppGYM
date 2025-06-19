/* VIEW: Medidas ---------------------------------------------------------
   Muestra el formulario de medidas y envía datos al backend.
----------------------------------------------------------------------- */
import { api } from "../api.js";
import { qs, nf } from "../../utils.js";

export async function loadStats(container) {
  container.innerHTML = /*html*/`
    <h2 class="view-title">Medidas</h2>
    <form id="stats-form" class="grid gap-4 max-w-xl">
      <input id="stats-date" type="date" class="input" style="width:max-content" required>

      <fieldset class="grid" style="grid-template-columns:repeat(3,1fr);gap:1rem;">
        <legend class="font-semibold">Medidas (cm)</legend>
        <input id="stats-weight"     type="number" step="0.1" class="input" placeholder="Peso (kg)">
        <input id="stats-neck"       type="number" class="input" placeholder="Cuello">
        <input id="stats-chest"      type="number" class="input" placeholder="Pecho">
        <input id="stats-waist"      type="number" class="input" placeholder="Cintura">
        <input id="stats-lowerAbs"   type="number" class="input" placeholder="Abd. bajo">
        <input id="stats-hip"        type="number" class="input" placeholder="Cadera">
        <input id="stats-biceps"     type="number" class="input" placeholder="Bíceps relaj.">
        <input id="stats-bicepsFlex" type="number" class="input" placeholder="Bíceps flex.">
        <input id="stats-forearm"    type="number" class="input" placeholder="Antebrazo">
        <input id="stats-thigh"      type="number" class="input" placeholder="Muslo">
        <input id="stats-calf"       type="number" class="input" placeholder="Pantorrilla">
      </fieldset>

      <button class="btn w-max">Guardar</button>
    </form>
  `;

  /* fecha hoy */
  qs("#stats-date").value = new Date().toISOString().slice(0, 10);

  /* envío */
  qs("#stats-form").onsubmit = async e => {
    e.preventDefault();
    const f = qs("#stats-form");
    await api.post("/api/stats", {
      date        : f["stats-date"].value,
      weightKg    : nf(f["stats-weight"].value),
      neckCm      : nf(f["stats-neck"].value),
      chestCm     : nf(f["stats-chest"].value),
      waistCm     : nf(f["stats-waist"].value),
      lowerAbsCm  : nf(f["stats-lowerAbs"].value),
      hipCm       : nf(f["stats-hip"].value),
      bicepsCm    : nf(f["stats-biceps"].value),
      bicepsFlexCm: nf(f["stats-bicepsFlex"].value),
      forearmCm   : nf(f["stats-forearm"].value),
      thighCm     : nf(f["stats-thigh"].value),
      calfCm      : nf(f["stats-calf"].value)
    });
    alert("Medidas guardadas");
    f.reset();
  };
}
