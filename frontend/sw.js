// assets/js/utils.js  (lugar único de registro SW)
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').then(reg => {
    // SW nuevo ya descargado y listo para activar
    if (reg.waiting) askRefresh(reg);

    // SW nuevo se descarga mientras la app está abierta
    reg.addEventListener('updatefound', () => {
      reg.installing.addEventListener('statechange', () => {
        if (reg.waiting) askRefresh(reg);
      });
    });
  });
}

function askRefresh(reg){
  reg.waiting.postMessage({type:'SKIP_WAITING'});       // activa
  reg.waiting.addEventListener('statechange', e=>{
    if(e.target.state==='activated') location.reload(); // página fresca
  });
}