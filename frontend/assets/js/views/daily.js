/* VIEW: Registro diario --------------------------------------------------
   – Lista de máquinas con edición de valores
   – Quick-Add se inicializa fuera (quickAdd.js)
   – Timer básico + avanzado (timer.js) ya insertados por main.js
----------------------------------------------------------------------- */
import { api } from "../api.js";
import { qs, create } from "../utils.js";

export async function loadDaily(container, machines) {
  container.innerHTML = /*html*/`
    <h2 class="view-title">Registro diario</h2>
    <form id="daily-form" class="grid gap-4">
      <input id="entry-date" type="date" class="input" style="width:max-content" required>
      <div id="daily-machines" class="grid gap-2"></div>
      <button class="btn w-max">Guardar registro</button>
    </form>
  `;

  const dateIn = qs("#entry-date");
  dateIn.value = new Date().toISOString().slice(0, 10);

  const box = qs("#daily-machines");
  renderRows(machines);

  /* guardado manual */
  qs("#daily-form").onsubmit = async e => {
    e.preventDefault();
    const ex = [];
    box.querySelectorAll(".row").forEach(r=>{
      ex.push({
        name : r.dataset.name,
        weightKg:+r.querySelector("[data-k]").value,
        reps:+r.querySelector("[data-r]").value,
        sets:+r.querySelector("[data-s]").value
      });
    });
    await api.post("/api/daily", { date: dateIn.value, exercises: ex });
    alert("Registro guardado");
  };

  /* helpers */
  function renderRows(list){
    box.innerHTML="";
    list.forEach(m=>{
      const row = create("div","flex gap-2 row");
      row.dataset.name = m.machine.name;
      row.innerHTML = `
        <span class="flex-1">${m.machine.name}</span>
        <input data-k type="number" class="input w-24" value="${m.weightKg}">
        <input data-r type="number" class="input w-16" value="${m.reps}">
        <input data-s type="number" class="input w-16" value="${m.sets}">
      `;
      /* timer básico: reinicia al cambiar reps */
      row.querySelector("[data-r]").addEventListener("change",
        () => document.dispatchEvent(new CustomEvent("series-changed")));
      box.appendChild(row);
    });
  }
}
