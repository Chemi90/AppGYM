import { qs } from "../utils.js";

export class Router {
  constructor(views, container){
    this.views = views;           // {hash: fn(container)}
    this.container = container;
    window.addEventListener("hashchange", () => this.render());
    this.render();
  }
  async render(){
    const hash = location.hash.slice(1) || "profile";
    if (!this.views[hash]) return;
    this.container.innerHTML = "";               // limpio
    await this.views[hash](this.container);      // pinta la vista
  }
}
