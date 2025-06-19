/* VIEW: Registro diario --------------------------------------------------
   – Lista editable de máquinas
   – Inyecta temporizador avanzado solo en esta vista
----------------------------------------------------------------------- */
import { api }                  from "../api.js";
import { qs, create }           from "../utils.js";
import { initAdvancedTimer }    from "../timer.js";

export async function loadDaily(container, machines) {

  /* ---------- HTML completo de la vista ---------- */
  container.innerHTML = /*html*/`
    <h2 class="view-title">Registro diario</h2>

    <form id="daily-form" class="grid gap-4">
      <input id="entry-date" type="date" class="input" style="width:max-content" required>
      <div id="daily-machines" class="grid gap-2"></div>
      <button class="btn w-max">Guardar registro</button>
    </form>

    <!-- zona exclusiva del temporizador -->
    <div id="timer-controls" class="mt-6"></div>
  `;

  /* ---------- fecha por defecto ---------- */
  qs("#entry-date").value = new Date().toISOString().slice(0, 10);

  /* ---------- pinta filas de máquinas ---------- */
  const box = qs("#daily-machines");
  machines.forEach(m => {
    const row = create("div","flex gap-2 row");
    row.dataset.name = m.machine.name;
    row.innerHTML = `
      <span class="flex-1">${m.machine.name}</span>
      <input data-k type="number" class="input w-24" value="${m.weightKg}">
      <input data-r type="number" class="input w-16" value="${m.reps}">
      <input data-s type="number" class="input w-16" value="${m.sets}">
    `;
    /* reinicia timer al cambiar reps */
    row.querySelector("[data-r]")
       .addEventListener("change", () => document.dispatchEvent(new Event("series-changed")));
    box.appendChild(row);
  });

  /* ---------- submit manual ---------- */
  qs("#daily-form").onsubmit = async e => {
    e.preventDefault();
    const ex = [];
    box.querySelectorAll(".row").forEach(r => {
      ex.push({
        name     : r.dataset.name,
        weightKg : +r.querySelector("[data-k]").value,
        reps     : +r.querySelector("[data-r]").value,
        sets     : +r.querySelector("[data-s]").value
      });
    });
    await api.post("/api/daily", {
      date      : qs("#entry-date").value,
      exercises : ex
    });
    alert("Registro guardado");
  };

  /* ---------- temporizador avanzado SOLO aquí ---------- */
  import("../timer.js").then(({ initAdvancedTimer }) =>
    initAdvancedTimer(qs("#timer-controls")));
}