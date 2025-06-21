import { qs, dbg } from "./utils.js";

export class Router {
  constructor (views, container) {
    this.views      = views;      // { hash : loaderFn }
    this.container  = container;
    window.addEventListener('hashchange', () => this.render());
    this.render();
  }

  async render () {
    const hash = location.hash.slice(1) || 'profile';
    dbg('ROUTER', 'hash →', hash);

    const load = this.views[hash];
    if (!load) { dbg('ROUTER', '404 view', hash); return; }

    this.container.innerHTML = '';
    await load(this.container);
  }
}
