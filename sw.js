const CACHE_NAME = 'tilawati-cache-v1';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './quran-data.js',
  './tafsir-data.js',
  './version.json',
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

// تثبيت ملف الخدمة وتخزين الأصول
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    }).then(() => self.skipWaiting())
  );
});

// تفعيل ملف الخدمة وحذف النسخ القديمة للكاش
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      );
    }).then(() => self.clients.claim())
  );
});

// اعتراض الطلبات وتقديمها من الكاش مع التحديث في الخلفية (Stale-While-Revalidate)
self.addEventListener('fetch', (event) => {
  // عدم تخزين الطلبات الخارجية الصوتية الكبيرة في الكاش الثابت
  if (event.request.url.includes('cdn.islamic.network')) {
    return;
  }

  // لملف version.json: نجلبه دائماً من الشبكة أولاً لمعرفة التحديثات فوراً
  if (event.request.url.includes('version.json')) {
    event.respondWith(
      fetch(event.request).catch(() => caches.match(event.request))
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      const fetchPromise = fetch(event.request).then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      }).catch(() => cachedResponse);

      return cachedResponse || fetchPromise;
    })
  );
});

