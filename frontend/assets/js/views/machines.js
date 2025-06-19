/* VIEW: Máquinas y pesos -----------------------------------------------
   CRUD en línea con edición fila-a-fila.
----------------------------------------------------------------------- */
import { api } from "../api.js";
import { qs, create } from "../utils.js";

export async function loadMachines(container) {
  container.innerHTML = /*html*/`
    <h2 class="view-title">Máquinas y pesos</h2>

    <form id="machine-form" class="flex flex-wrap gap-4 mb-4">
      <input id="machine-name" class="input flex-1" placeholder="Nombre de la máquina" required>
      <input id="machine-kg"   type="number" class="input" style="width:6rem" placeholder="Kg"    required>
      <input id="machine-reps" type="number" class="input" style="width:5rem" placeholder="Reps"  required>
      <input id="machine-sets" type="number" class="input" style="width:5rem" placeholder="Series" required>
      <button class="btn">Añadir / Actualizar</button>
    </form>

    <table class="bg-white shadow rounded-xl overflow-hidden">
      <thead>
        <tr>
          <th class="th">Máquina</th><th class="th">Kg</th>
          <th class="th">Reps</th><th class="th">Series</th><th class="th"></th>
        </tr>
      </thead>
      <tbody id="machine-table"></tbody>
    </table>
  `;

  const table = qs("#machine-table");
  renderRows(await api.get("/api/machines"));

  /* alta / update simple */
  qs("#machine-form").onsubmit = async e => {
    e.preventDefault();
    const f = e.target;
    await api.post("/api/machines", {
      name   : f["machine-name"].value,
      weightKg:+f["machine-kg"].value,
      reps   : +f["machine-reps"].value,
      sets   : +f["machine-sets"].value
    });
    f.reset();
    renderRows(await api.get("/api/machines"));
  };

  /* helpers */
  function renderRows(rows) {
    table.innerHTML = "";
    rows.forEach(m => {
      const tr = create("tr");
      tr.innerHTML = `
        <td class="machine-name">${m.machine.name}</td>
        <td class="machine-kg">${m.weightKg}</td>
        <td class="machine-reps">${m.reps}</td>
        <td class="machine-sets">${m.sets}</td>
        <td class="text-right">
          <button class="btn-icon btn-edit" title="Editar">✎</button>
          <button class="btn-danger"        title="Eliminar">×</button>
        </td>`;
      table.appendChild(tr);

      /* eliminar */
      tr.querySelector(".btn-danger").onclick = async () => {
        await api.del(`/api/machines/${m.id}`);
        tr.remove();
      };

      /* editar */
      tr.querySelector(".btn-edit").onclick = () => startEdit(tr, m);
    });
  }

  function startEdit(tr, m) {
    tr.querySelector(".machine-kg").innerHTML   = `<input type="number" class="input w-24" value="${m.weightKg}">`;
    tr.querySelector(".machine-reps").innerHTML = `<input type="number" class="input w-16" value="${m.reps}">`;
    tr.querySelector(".machine-sets").innerHTML = `<input type="number" class="input w-16" value="${m.sets}">`;

    const tdAct = tr.lastElementChild;
    tdAct.querySelector(".btn-edit").remove();
    const save = create("button", "btn"); save.textContent = "Guardar"; tdAct.prepend(save);

    save.onclick = async () => {
      const kg   = +tr.querySelector(".machine-kg input").value;
      const reps = +tr.querySelector(".machine-reps input").value;
      const sets = +tr.querySelector(".machine-sets input").value;
      await api.post("/api/machines", { name: m.machine.name, weightKg: kg, reps, sets });
      renderRows(await api.get("/api/machines"));
    };
  }
}
