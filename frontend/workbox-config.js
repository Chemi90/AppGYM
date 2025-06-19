module.exports = {
  globDirectory : './',
  globPatterns  : [
    '**/*.{css,js,html,webmanifest,json,png,svg}'
  ],

  swDest : 'sw.js',
  ignoreURLParametersMatching: [/^utm_/, /^fbclid$/],

  runtimeCaching: [
    {
      urlPattern: ({request}) => request.mode === 'navigate',
      handler   : 'NetworkFirst',
      options   : { cacheName: 'pages' }
    },
    {
      urlPattern: ({url, request}) =>
        url.pathname.startsWith('/api/') && request.method === 'GET',
      handler   : 'StaleWhileRevalidate',
      options   : { cacheName: 'api' }
    }
  ]
};
