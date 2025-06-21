/* ------------------------------------------------------------------
   Workbox 7 - Config · Gym-Tracker PWA
   ------------------------------------------------------------------ */
module.exports = {
  /* —— Qué ficheros precachear ——————————————— */
  globDirectory : './',
  globPatterns  : [
    '**/*.{css,js,html,webmanifest,json,png,svg}'
  ],

  /* —— Salida del service-worker ———————————— */
  swDest  : 'sw.js',

  /* —— Parámetros a ignorar en la URL ————————— */
  ignoreURLParametersMatching: [/^utm_/, /^fbclid$/],

  /* —— Estrategias runtime (lo que NO va en precache) ———————— */
  runtimeCaching: [
    /* Páginas de navegación (SPA) */
    {
      urlPattern: ({request}) => request.mode === 'navigate',
      handler   : 'NetworkFirst',
      options   : { cacheName: 'pages' }
    },

    /* CSS actualizado en caliente  */
    {
      urlPattern: ({request}) => request.destination === 'style',
      handler   : 'NetworkFirst',
      options   : { cacheName: 'styles' }
    },

    /* Llamadas GET a tu API */
    {
      urlPattern: ({url, request}) =>
        url.pathname.startsWith('/api/') && request.method === 'GET',
      handler   : 'StaleWhileRevalidate',
      options   : { cacheName: 'api' }
    }
  ],

  /* —— Borra caches antiguas que ya no estén listadas ———————— */
  cleanupOutdatedCaches: true
};
