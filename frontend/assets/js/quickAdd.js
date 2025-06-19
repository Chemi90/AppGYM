/* Quick-Add — botón flotante ＋
   Inserta la última configuración de cada máquina como
   entrada “Diaria” con la fecha de hoy
------------------------------------------------------------------ */
import { api } from "./api.js";
import { qs }  from "../utils.js";

export async function initQuickAdd(listMachines) {
  qs("#quick-add-btn").onclick = async () => {
    const today     = new Date().toISOString().slice(0, 10);
    const exercises = listMachines.map(m => ({
      name     : m.machine.name,
      weightKg : m.weightKg,
      reps     : m.reps,
      sets     : m.sets
    }));
    await api.post("/api/daily", { date: today, exercises });
    alert("Registro rápido guardado ✔️");
  };
}
