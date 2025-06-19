import { api } from "../api.js";
import { qs } from "../../utils.js";

export async function loadProfile(container){
  container.innerHTML = `
    <h2 class="view-title">Perfil</h2>
    <form id="profile-form" class="grid gap-4 max-w-xl">
      <div class="grid" style="grid-template-columns:1fr 1fr;gap:1rem;">
        <input id="firstName" class="input" placeholder="Nombre" required>
        <input id="lastName"  class="input" placeholder="Apellidos" required>
      </div>
      <div class="grid" style="grid-template-columns:repeat(3,1fr);gap:1rem;">
        <input id="age"    type="number" class="input" placeholder="Edad" required>
        <input id="height" type="number" class="input" placeholder="Estatura (cm)" required>
        <input id="weight" type="number" class="input" placeholder="Peso (kg)" required>
      </div>
      <button class="btn w-max">Guardar</button>
    </form>
  `;
  const data = await api.get("/api/profile");
  const form = qs("#profile-form");
  Object.entries({
    firstName:data.firstName,lastName:data.lastName,age:data.age,
    height:data.heightCm,weight:data.weightKg
  }).forEach(([id,v])=>{if(v!=null)form[id].value=v;});
  form.onsubmit = async e=>{
    e.preventDefault();
    await api.put("/api/profile",{
      firstName:form.firstName.value,lastName:form.lastName.value,
      age:+form.age.value,heightCm:+form.height.value,weightKg:+form.weight.value
    });
    alert("Perfil actualizado");
  };
}
