/** Build-time worker source. Never caches API data or personal workout records. */
export const workerSource = (assets, version) => `
const CACHE=${JSON.stringify("onepunch-shell-" + version)};
const ASSETS=${JSON.stringify(["/", ...assets])};
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS))));
self.addEventListener('activate',event=>event.waitUntil(Promise.all([caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('onepunch-shell-')&&key!==CACHE).map(key=>caches.delete(key)))),self.clients.claim()])));
self.addEventListener('fetch',event=>{
 const url=new URL(event.request.url);
 if(event.request.method!=='GET'||url.origin!==self.location.origin||url.pathname.startsWith('/api/'))return;
 if(event.request.mode==='navigate'){
  event.respondWith(fetch(event.request).catch(()=>caches.open(CACHE).then(cache=>cache.match('/'))));return;
 }
 if(ASSETS.includes(url.pathname))event.respondWith(caches.open(CACHE).then(async cache=>await cache.match(event.request)||fetch(event.request)));
});
`;
