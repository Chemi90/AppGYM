import { precacheAndRoute } from 'workbox-precaching';
import { registerRoute }    from 'workbox-routing';
import { NetworkFirst, StaleWhileRevalidate } from 'workbox-strategies';

precacheAndRoute(self.__WB_MANIFEST);

registerRoute(({request}) => request.mode === 'navigate',
              new NetworkFirst({ cacheName: 'pages' }));

registerRoute(({url, request}) =>
  url.pathname.startsWith('/api/') && request.method === 'GET',
  new StaleWhileRevalidate({ cacheName: 'api' }));
