/* VIEW: Registro diario – cronómetro arriba del todo
----------------------------------------------------------------------- */
import { api } from "../api.js";
import { qs, create } from "../utils.js";
import { initAdvancedTimer } from "../timer.js";

export async function loadDaily(container, machines) {
  container.innerHTML = `
    <h2 class="view-title">Registro diario</h2>
    <div id="timer-anchor"></div>

    <form id="daily-form" class="grid gap-4">
      <input id="entry-date" type="date" class="input" style="width:max-content" required>
      <div id="daily-machines" class="grid gap-2"></div>
      <button class="btn w-max">Guardar registro</button>
    </form>
  `;

  /* poner temporizador inmediatamente debajo del título */
  initAdvancedTimer(qs("#timer-anchor"));

  qs("#entry-date").value = new Date().toISOString().slice(0, 10);

  const box = qs("#daily-machines");
  renderRows(machines);

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
    await api.post("/api/daily", { date: qs("#entry-date").value, exercises: ex });
    alert("Registro guardado");
  };

  function renderRows(list) {
    box.innerHTML = "";
    list.forEach(m => {
      const row = create("div", "flex gap-2 row");
      row.dataset.name = m.machine.name;
      row.innerHTML = `
        <span class="flex-1">${m.machine.name}</span>
        <input data-k type="number" class="input w-24" value="${m.weightKg}">
        <input data-r type="number" class="input w-16" value="${m.reps}">
        <input data-s type="number" class="input w-16" value="${m.sets}">
      `;
      row.querySelector("[data-r]").addEventListener("change",
        () => document.dispatchEvent(new CustomEvent("series-changed")));
      box.appendChild(row);
    });
  }
}
