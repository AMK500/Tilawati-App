/**
 * تطبيق «تلاوتي» - المنطق البرمجي الكامل
 * يدعم:
 * 1. وضع التقليب الجانبي (الأفقي كالمصحف الشريف)
 * 2. وضع التقليب والتمرير العلوي المستمر
 * 3. ميزة الحركة التلقائية مع التحكم الدقيق بالسرعة والإيقاف المؤقت الذكي
 * 4. الثيمات القرآنية الخمس
 * 5. التلاوة الصوتية وتظليل الآيات
 * 6. التفسير الميسر بلمسة واحدة
 * 7. دعم الشاشات الكبيرة وتكبير الخط لكبار السن
 * 8. دعم تدوير الهاتف بالعرض (Landscape)
 */

// حالة التطبيق العامة
const CURRENT_APP_VERSION = "1.0.0";

const state = {
  currentSurahId: 1,
  currentMode: 'horizontal', // 'horizontal' أو 'vertical'
  surahData: null,
  activeAyahNumber: null,
  
  // إعدادات الحركة التلقائية
  autoScroll: {
    isActive: false,
    speed: 1.5,
    animationId: null,
    userInteracting: false,
    interactionTimeout: null
  },
  
  // إعدادات العرض
  theme: 'classic',
  fontSize: 26,
  fontFamily: "'Amiri Quran', serif",
  
  // إعدادات الصوت
  reciter: 'ar.alafasy',
  isPlayingAudio: false,
  currentAudioAyahIndex: 0,
  
  // الفواصل والذاكرة
  bookmarks: []
};

// عناصر واجهة المستخدم
const DOM = {
  body: document.body,
  surahTitleDisplay: document.getElementById('surahTitleDisplay'),
  surahMetaDisplay: document.getElementById('surahMetaDisplay'),
  headerSurahSubtitle: document.getElementById('headerSurahSubtitle'),
  bismillahContainer: document.getElementById('bismillahContainer'),
  quranTextContainer: document.getElementById('quranTextContainer'),
  flipControlsWrapper: document.getElementById('flipControlsWrapper'),
  flipPageIndicator: document.getElementById('flipPageIndicator'),
  prevPageBtn: document.getElementById('prevPageBtn'),
  nextPageBtn: document.getElementById('nextPageBtn'),
  tabHorizontalMode: document.getElementById('tabHorizontalMode'),
  tabVerticalMode: document.getElementById('tabVerticalMode'),
  autoScrollDock: document.getElementById('autoScrollDock'),
  toggleAutoScrollBtn: document.getElementById('toggleAutoScrollBtn'),
  playIcon: document.getElementById('playIcon'),
  speedSlider: document.getElementById('speedSlider'),
  speedDisplayVal: document.getElementById('speedDisplayVal'),
  speedUpBtn: document.getElementById('speedUpBtn'),
  speedDownBtn: document.getElementById('speedDownBtn'),
  badgeSurahType: document.getElementById('badgeSurahType'),
  badgeVersesCount: document.getElementById('badgeVersesCount'),
  badgeOrientationNotice: document.getElementById('badgeOrientationNotice'),
  
  // الأدراج والنوافذ
  drawerBackdrop: document.getElementById('drawerBackdrop'),
  surahIndexDrawer: document.getElementById('surahIndexDrawer'),
  themesDrawer: document.getElementById('themesDrawer'),
  settingsDrawer: document.getElementById('settingsDrawer'),
  bookmarksDrawer: document.getElementById('bookmarksDrawer'),
  duaaDrawer: document.getElementById('duaaDrawer'),
  tafsirSheet: document.getElementById('tafsirSheet'),
  
  // أزرار الهيدر
  openIndexBtn: document.getElementById('openIndexBtn'),
  openThemesBtn: document.getElementById('openThemesBtn'),
  openSettingsBtn: document.getElementById('openSettingsBtn'),
  openBookmarksBtn: document.getElementById('openBookmarksBtn'),
  openDuaaBtn: document.getElementById('openDuaaBtn'),
  toggleFullscreenBtn: document.getElementById('toggleFullscreenBtn'),
  
  // عناصر التفسير
  tafsirTitle: document.getElementById('tafsirTitle'),
  tafsirVerseText: document.getElementById('tafsirVerseText'),
  tafsirExplanation: document.getElementById('tafsirExplanation'),
  playAyahAudioBtn: document.getElementById('playAyahAudioBtn'),
  copyAyahBtn: document.getElementById('copyAyahBtn'),
  shareAyahBtn: document.getElementById('shareAyahBtn'),
  
  // مشغل الصوت
  quranAudioElement: document.getElementById('quranAudioElement'),
  audioPlayPauseBtn: document.getElementById('audioPlayPauseBtn'),
  stopAudioBtn: document.getElementById('stopAudioBtn'),
  reciterSelect: document.getElementById('reciterSelect'),
  currentReciterName: document.getElementById('currentReciterName'),
  audioVerseStatus: document.getElementById('audioVerseStatus'),
  
  // الإعدادات
  fontSizeSlider: document.getElementById('fontSizeSlider'),
  fontIncBtn: document.getElementById('fontIncBtn'),
  fontDecBtn: document.getElementById('fontDecBtn'),
  fontFamilySelect: document.getElementById('fontFamilySelect'),
  
  // البحث والفواصل
  surahSearchInput: document.getElementById('surahSearchInput'),
  surahListContainer: document.getElementById('surahListContainer'),
  saveCurrentBookmarkBtn: document.getElementById('saveCurrentBookmarkBtn'),
  bookmarksListContainer: document.getElementById('bookmarksListContainer'),
  khatmDuaaContainer: document.getElementById('khatmDuaaContainer')
};

// تهيئة التطبيق عند اكتمال تحميل الصفحة
document.addEventListener('DOMContentLoaded', () => {
  loadStoredPreferences();
  populateRecitersList();
  renderSurahIndexList();
  renderKhatmDuaa();
  setupEventListeners();
  checkOrientation();
  loadSurah(state.currentSurahId);
});

// استعادة التفضيلات المخزنة
function loadStoredPreferences() {
  const savedTheme = localStorage.getItem('tilawati_theme');
  if (savedTheme) applyTheme(savedTheme);

  const savedSize = localStorage.getItem('tilawati_fontSize');
  if (savedSize) {
    state.fontSize = parseInt(savedSize, 10);
    applyFontSize(state.fontSize);
  }

  const savedMode = localStorage.getItem('tilawati_mode');
  if (savedMode && (savedMode === 'horizontal' || savedMode === 'vertical')) {
    setReadingMode(savedMode);
  }

  const savedSurah = localStorage.getItem('tilawati_lastSurah');
  if (savedSurah) {
    state.currentSurahId = parseInt(savedSurah, 10);
  }

  const savedSpeed = localStorage.getItem('tilawati_scrollSpeed');
  if (savedSpeed) {
    state.autoScroll.speed = parseFloat(savedSpeed);
    DOM.speedSlider.value = state.autoScroll.speed;
    updateSpeedLabel();
  }

  const savedBookmarks = localStorage.getItem('tilawati_bookmarks');
  if (savedBookmarks) {
    try {
      state.bookmarks = JSON.parse(savedBookmarks);
      renderBookmarksList();
    } catch(e) {}
  }
}

// تبديل وضع القراءة: أفقي (جانبي) مقابل عمودي (علوي)
function setReadingMode(mode) {
  state.currentMode = mode;
  localStorage.setItem('tilawati_mode', mode);

  if (mode === 'horizontal') {
    DOM.tabHorizontalMode.classList.add('active');
    DOM.tabVerticalMode.classList.remove('active');
    DOM.flipControlsWrapper.style.display = 'flex';
    DOM.autoScrollDock.style.display = 'none';
    stopAutoScroll();
  } else {
    DOM.tabHorizontalMode.classList.remove('active');
    DOM.tabVerticalMode.classList.add('active');
    DOM.flipControlsWrapper.style.display = 'none';
    DOM.autoScrollDock.style.display = 'flex';
  }
}

// تحميل وعرض بيانات السورة
async function loadSurah(surahId) {
  state.currentSurahId = surahId;
  localStorage.setItem('tilawati_lastSurah', surahId);
  stopAudio();

  const meta = SURAH_METADATA.find(s => s.id === surahId) || SURAH_METADATA[0];
  DOM.surahTitleDisplay.textContent = `سُورَةُ ${meta.name}`;
  DOM.surahMetaDisplay.textContent = `ترتيبها: ${meta.id} • آياتها: ${meta.totalVerses} • ${meta.type} • صفحة ${meta.startPage}`;
  DOM.headerSurahSubtitle.textContent = `سورة ${meta.name}`;
  DOM.badgeSurahType.textContent = meta.type;
  DOM.badgeVersesCount.textContent = `${meta.totalVerses} آيات`;
  DOM.flipPageIndicator.textContent = `سورة ${meta.id} من 114`;

  // إخفاء البسملة في سورة التوبة (رقم 9) وسورة الفاتحة مدمجة في أول آية
  if (surahId === 9) {
    DOM.bismillahContainer.style.display = 'none';
  } else if (surahId === 1) {
    DOM.bismillahContainer.style.display = 'none';
  } else {
    DOM.bismillahContainer.style.display = 'block';
  }

  // تحديث حالة أزرار التالي والسابق
  DOM.prevPageBtn.disabled = (surahId <= 1);
  DOM.nextPageBtn.disabled = (surahId >= 114);

  // عرض مؤشر التحميل الخفيف
  DOM.quranTextContainer.innerHTML = `
    <div style="text-align: center; padding: 40px; color: var(--text-secondary); font-family: var(--font-ui);">
      <div style="font-size: 24px; margin-bottom: 8px;">⏳</div>
      <span>جاري تحميل آيات سورة ${meta.name}...</span>
    </div>
  `;

  let surah = null;

  // 1. فحص السور المدمجة محلياً أولاً لسرعة فائقة ودون الحاجة للإنترنت
  if (BUILTIN_SURAHS[surahId]) {
    surah = BUILTIN_SURAHS[surahId];
  } else {
    // 2. فحص التخزين المؤقت في المتصفح
    const cached = localStorage.getItem(`surah_${surahId}`);
    if (cached) {
      try {
        surah = JSON.parse(cached);
      } catch(e) {}
    }
  }

  // 3. إذا لم تكن مخزنة، جلبها من شبكة القرآن الكريم وتخزينها
  if (!surah) {
    try {
      const response = await fetch(`https://api.alquran.cloud/v1/surah/${surahId}/quran-uthmani`);
      if (response.ok) {
        const json = await response.json();
        if (json && json.data) {
          surah = json.data;
          try {
            localStorage.setItem(`surah_${surahId}`, JSON.stringify(surah));
          } catch(e) {
            console.warn("مساحة التخزين ممتلئة:", e);
          }
        }
      }
    } catch(err) {
      console.warn("تعذر جلب السورة عبر الإنترنت، استخدام النص التقريبي:", err);
    }
  }

  // 4. إذا فشل كل شيء، إنشاء محتوى آيات استرشادي
  if (!surah) {
    surah = {
      number: surahId,
      name: `سورة ${meta.name}`,
      ayahs: Array.from({ length: meta.totalVerses }, (_, i) => ({
        numberInSurah: i + 1,
        text: `الآية الكريمة رقم ${i + 1} من سورة ${meta.name} المباركة.`
      }))
    };
  }

  state.surahData = surah;
  renderSurahText(surah);
  highlightActiveSurahInList(surahId);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// رسم الآيات القرآنية داخل الحاوية
function renderSurahText(surah) {
  let html = '';
  surah.ayahs.forEach((ayah, index) => {
    // تنظيف البسملة المتكررة في بداية السورة الأولى عدا الفاتحة
    let text = ayah.text;
    if (ayah.numberInSurah === 1 && surah.number !== 1 && surah.number !== 9) {
      text = text.replace(/^بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ\s*/, '');
    }

    html += `
      <span class="verse-item" id="ayah-${ayah.numberInSurah}" data-surah="${surah.number}" data-ayah="${ayah.numberInSurah}" data-index="${index}">
        ${text}
        <span class="ayah-sign">${convertToArabicNumerals(ayah.numberInSurah)}</span>
      </span>
    `;
  });

  DOM.quranTextContainer.innerHTML = html;

  // إضافة أحداث النقر على الآيات لعرض التفسير السريع
  document.querySelectorAll('.verse-item').forEach(el => {
    el.addEventListener('click', (e) => {
      const sNum = parseInt(el.dataset.surah, 10);
      const aNum = parseInt(el.dataset.ayah, 10);
      const aIndex = parseInt(el.dataset.index, 10);
      openTafsirSheet(sNum, aNum, aIndex);
    });
  });
}

// تحويل الأرقام إلى أرقام عربية مشرقية (١، ٢، ٣)
function convertToArabicNumerals(num) {
  const arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  return num.toString().split('').map(d => arabicDigits[parseInt(d, 10)] || d).join('');
}

// ==========================================================================
// محرك الحركة والتمرير التلقائي (Auto-Scroll Engine)
// ==========================================================================

function toggleAutoScroll() {
  if (state.autoScroll.isActive) {
    stopAutoScroll();
  } else {
    startAutoScroll();
  }
}

function startAutoScroll() {
  state.autoScroll.isActive = true;
  DOM.toggleAutoScrollBtn.style.background = 'linear-gradient(135deg, #2e7d32, #4caf50)';
  DOM.playIcon.textContent = '⏸';
  DOM.toggleAutoScrollBtn.title = 'إيقاف مؤقت للحركة التلقائية';
  
  runAutoScrollLoop();
}

function stopAutoScroll() {
  state.autoScroll.isActive = false;
  if (state.autoScroll.animationId) {
    cancelAnimationFrame(state.autoScroll.animationId);
    state.autoScroll.animationId = null;
  }
  DOM.toggleAutoScrollBtn.style.background = '';
  DOM.playIcon.textContent = '▶';
  DOM.toggleAutoScrollBtn.title = 'بدء الحركة التلقائية';
}

function runAutoScrollLoop() {
  if (!state.autoScroll.isActive) return;

  // إذا كان المستخدم يتفاعل باللمس أو الفأرة، توقف مؤقتاً
  if (!state.autoScroll.userInteracting) {
    window.scrollBy({
      top: state.autoScroll.speed,
      behavior: 'auto'
    });

    // التحقق عند الوصول إلى نهاية الصفحة
    const scrollBottom = window.innerHeight + window.scrollY;
    if (scrollBottom >= document.documentElement.scrollHeight - 5) {
      // إيقاف أو الانتقال للسورة التالية إذا رغب المستخدم
      stopAutoScroll();
      return;
    }
  }

  state.autoScroll.animationId = requestAnimationFrame(runAutoScrollLoop);
}

function updateSpeedLabel() {
  DOM.speedDisplayVal.textContent = `سرعة: ${state.autoScroll.speed}x`;
  localStorage.setItem('tilawati_scrollSpeed', state.autoScroll.speed);
}

// ==========================================================================
// مشغل التلاوة الصوتي وتظليل الآيات المتزامن
// ==========================================================================

function populateRecitersList() {
  DOM.reciterSelect.innerHTML = RECITERS.map(r => `
    <option value="${r.id}">${r.name} (${r.style})</option>
  `).join('');
  
  DOM.reciterSelect.addEventListener('change', (e) => {
    state.reciter = e.target.value;
    const rObj = RECITERS.find(r => r.id === state.reciter);
    DOM.currentReciterName.textContent = `بصوت: ${rObj ? rObj.name : ''}`;
    if (state.isPlayingAudio) {
      playAyahAudio(state.currentAudioAyahIndex);
    }
  });
}

function toggleAudio() {
  if (state.isPlayingAudio) {
    pauseAudio();
  } else {
    playAyahAudio(state.currentAudioAyahIndex || 0);
  }
}

async function playAyahAudio(index) {
  if (!state.surahData || !state.surahData.ayahs || index >= state.surahData.ayahs.length) {
    stopAudio();
    return;
  }

  state.currentAudioAyahIndex = index;
  const ayah = state.surahData.ayahs[index];
  state.activeAyahNumber = ayah.numberInSurah;

  // تظليل الآية الحالية وإزالة التظليل عن الباقي
  document.querySelectorAll('.verse-item').forEach(el => el.classList.remove('active-audio'));
  const currentEl = document.getElementById(`ayah-${ayah.numberInSurah}`);
  if (currentEl) {
    currentEl.classList.add('active-audio');
    currentEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  DOM.audioVerseStatus.textContent = `يتلو الآية (${ayah.numberInSurah}) من سورة ${state.surahData.name}`;
  DOM.audioPlayPauseBtn.textContent = '⏸';
  state.isPlayingAudio = true;

  // جلب رابط الصوت للآية
  try {
    const audioUrl = `https://cdn.islamic.network/quran/audio/128/${state.reciter}/${calculateGlobalAyahNumber(state.currentSurahId, ayah.numberInSurah)}.mp3`;
    DOM.quranAudioElement.src = audioUrl;
    DOM.quranAudioElement.play().catch(e => {
      console.warn("تنبيه تشغيل الصوت:", e);
    });

    DOM.quranAudioElement.onended = () => {
      // الانتقال التلقائي للآية التالية
      if (state.currentAudioAyahIndex + 1 < state.surahData.ayahs.length) {
        playAyahAudio(state.currentAudioAyahIndex + 1);
      } else {
        stopAudio();
      }
    };
  } catch(err) {
    console.error("خطأ في تشغيل الصوت:", err);
  }
}

function pauseAudio() {
  state.isPlayingAudio = false;
  DOM.quranAudioElement.pause();
  DOM.audioPlayPauseBtn.textContent = '▶';
  DOM.audioVerseStatus.textContent = 'تلاوة متوقفة مؤقتاً';
}

function stopAudio() {
  state.isPlayingAudio = false;
  DOM.quranAudioElement.pause();
  DOM.quranAudioElement.currentTime = 0;
  DOM.audioPlayPauseBtn.textContent = '▶';
  DOM.audioVerseStatus.textContent = 'اضغط للاستماع للسورة الحالية';
  document.querySelectorAll('.verse-item').forEach(el => el.classList.remove('active-audio'));
}

// حساب رقم الآية الإجمالي في المصحف (من 1 إلى 6236) لتشغيل الملف الصوتي بدقة
function calculateGlobalAyahNumber(surahId, ayahNumberInSurah) {
  let count = 0;
  for (let i = 1; i < surahId; i++) {
    const meta = SURAH_METADATA.find(s => s.id === i);
    if (meta) count += meta.totalVerses;
  }
  return count + ayahNumberInSurah;
}

// ==========================================================================
// نافذة التفسير الميسر المنبثقة السريعة
// ==========================================================================

async function openTafsirSheet(surahNumber, ayahNumber, ayahIndex) {
  const surahMeta = SURAH_METADATA.find(s => s.id === surahNumber);
  const ayahObj = state.surahData.ayahs[ayahIndex];
  
  DOM.tafsirTitle.textContent = `تفسير سورة ${surahMeta.name} - الآية (${ayahNumber})`;
  DOM.tafsirVerseText.textContent = ayahObj.text;
  DOM.tafsirExplanation.innerHTML = `<em>جاري جلب التفسير الميسر...</em>`;
  DOM.tafsirSheet.classList.add('open');
  DOM.drawerBackdrop.classList.add('open');

  // جلب التفسير
  const tafsirText = await getAyahTafsir(surahNumber, ayahNumber);
  DOM.tafsirExplanation.textContent = tafsirText;

  // ربط الأزرار السريعة
  DOM.playAyahAudioBtn.onclick = () => {
    closeAllDrawers();
    playAyahAudio(ayahIndex);
  };

  DOM.copyAyahBtn.onclick = () => {
    const textToCopy = `﴿${ayahObj.text}﴾ [سورة ${surahMeta.name}: الآية ${ayahNumber}] - برنامج تلاوتي`;
    navigator.clipboard.writeText(textToCopy).then(() => {
      alert("تم نسخ الآية الكريمة بنجاح!");
    });
  };

  DOM.shareAyahBtn.onclick = () => {
    const textToShare = `﴿${ayahObj.text}﴾\n[سورة ${surahMeta.name}: الآية ${ayahNumber}]\nالتفسير الميسر: ${tafsirText}\n- عبر تطبيق تلاوتي`;
    if (navigator.share) {
      navigator.share({
        title: `آية من سورة ${surahMeta.name}`,
        text: textToShare
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(textToShare).then(() => {
        alert("تم نسخ نص الآية مع التفسير لمشاركتها!");
      });
    }
  };
}

// ==========================================================================
// إدارة الثيمات القرآنية الخمسة
// ==========================================================================

function applyTheme(themeName) {
  state.theme = themeName;
  DOM.body.setAttribute('data-theme', themeName);
  localStorage.setItem('tilawati_theme', themeName);

  document.querySelectorAll('.theme-card').forEach(card => {
    if (card.dataset.setTheme === themeName) {
      card.classList.add('active');
    } else {
      card.classList.remove('active');
    }
  });
}

// إدارة حجم الخط وإمكانية الرؤية لكبار السن
function applyFontSize(size) {
  state.fontSize = size;
  document.documentElement.style.setProperty('--verse-font-size', `${size}px`);
  DOM.fontSizeSlider.value = size;
  localStorage.setItem('tilawati_fontSize', size);
}

// ==========================================================================
// الفهرس والبحث في السور
// ==========================================================================

function renderSurahIndexList() {
  DOM.surahListContainer.innerHTML = SURAH_METADATA.map(s => `
    <div class="surah-list-item ${s.id === state.currentSurahId ? 'active' : ''}" data-id="${s.id}">
      <div>
        <strong>${s.id}. سورة ${s.name}</strong>
        <div class="surah-sub-info" style="font-size: 11px; color: var(--text-secondary);">${s.type} • ${s.totalVerses} آيات</div>
      </div>
      <div style="font-size: 12px; font-weight: 700; color: var(--text-accent);">ص ${s.startPage}</div>
    </div>
  `).join('');

  DOM.surahListContainer.querySelectorAll('.surah-list-item').forEach(el => {
    el.addEventListener('click', () => {
      const id = parseInt(el.dataset.id, 10);
      loadSurah(id);
      closeAllDrawers();
    });
  });
}

function filterSurahIndex(query) {
  const cleanQ = query.trim().replace(/[ً-ٟ]/g, '').toLowerCase();
  DOM.surahListContainer.querySelectorAll('.surah-list-item').forEach(el => {
    const text = el.textContent.replace(/[ً-ٟ]/g, '').toLowerCase();
    if (text.includes(cleanQ)) {
      el.style.display = 'flex';
    } else {
      el.style.display = 'none';
    }
  });
}

function highlightActiveSurahInList(id) {
  DOM.surahListContainer.querySelectorAll('.surah-list-item').forEach(el => {
    if (parseInt(el.dataset.id, 10) === id) {
      el.classList.add('active');
    } else {
      el.classList.remove('active');
    }
  });
}

// ==========================================================================
// الفواصل والعلامات المرجعية
// ==========================================================================

function saveCurrentBookmark() {
  const meta = SURAH_METADATA.find(s => s.id === state.currentSurahId);
  const now = new Date();
  const dateStr = now.toLocaleDateString('ar-EG', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

  const newBookmark = {
    id: Date.now(),
    surahId: state.currentSurahId,
    surahName: meta.name,
    page: meta.startPage,
    date: dateStr
  };

  state.bookmarks.unshift(newBookmark);
  if (state.bookmarks.length > 20) state.bookmarks.pop();
  localStorage.setItem('tilawati_bookmarks', JSON.stringify(state.bookmarks));
  renderBookmarksList();
  alert(`تم حفظ الفاصلة عند سورة ${meta.name} بنجاح!`);
}

function renderBookmarksList() {
  if (state.bookmarks.length === 0) {
    DOM.bookmarksListContainer.innerHTML = `
      <div style="text-align: center; color: var(--text-secondary); font-size: 13px; padding: 20px;">
        لا توجد فواصل محفوظة بعد. اضغط على الزر أعلاه لإضافة فاصلة لموضع قراءتك.
      </div>
    `;
    return;
  }

  DOM.bookmarksListContainer.innerHTML = state.bookmarks.map(b => `
    <div class="surah-list-item" style="margin-bottom: 8px;">
      <div onclick="loadSurah(${b.surahId}); closeAllDrawers();" style="flex:1; cursor: pointer;">
        <strong>🔖 سورة ${b.surahName}</strong>
        <div style="font-size: 11px; color: var(--text-secondary);">صفحة ${b.page} • ${b.date}</div>
      </div>
      <button class="close-modal-btn" onclick="removeBookmark(${b.id})" title="حذف الفاصلة" style="padding: 4px 8px;">🗑️</button>
    </div>
  `).join('');
}

window.removeBookmark = function(id) {
  state.bookmarks = state.bookmarks.filter(b => b.id !== id);
  localStorage.setItem('tilawati_bookmarks', JSON.stringify(state.bookmarks));
  renderBookmarksList();
};

// ==========================================================================
// دعاء ختم القرآن الكريم
// ==========================================================================

function renderKhatmDuaa() {
  DOM.khatmDuaaContainer.innerHTML = KHATM_DUAA.map((duaa, i) => `
    <div class="duaa-card">
      <span style="color: var(--text-accent); font-weight: bold; margin-left: 6px;">(${convertToArabicNumerals(i + 1)})</span>
      ${duaa}
    </div>
  `).join('');
}

// ==========================================================================
// دعم الهاتف بالعرض (Landscape Orientation) والشاشات الكبيرة
// ==========================================================================

function checkOrientation() {
  const isLandscape = window.innerWidth > window.innerHeight;
  if (isLandscape && window.innerHeight < 600) {
    DOM.badgeOrientationNotice.style.display = 'inline-block';
  } else {
    DOM.badgeOrientationNotice.style.display = 'none';
  }
}

// ==========================================================================
// معالجة الأحداث (Event Listeners & Gesture Controls)
// ==========================================================================

function setupEventListeners() {
  // تبديل أوضاع القراءة
  DOM.tabHorizontalMode.addEventListener('click', () => setReadingMode('horizontal'));
  DOM.tabVerticalMode.addEventListener('click', () => setReadingMode('vertical'));

  // أزرار التنقل في وضع التقليب الجانبي
  DOM.nextPageBtn.addEventListener('click', () => {
    if (state.currentSurahId < 114) loadSurah(state.currentSurahId + 1);
  });
  DOM.prevPageBtn.addEventListener('click', () => {
    if (state.currentSurahId > 1) loadSurah(state.currentSurahId - 1);
  });

  // التحكم بالحركة التلقائية
  DOM.toggleAutoScrollBtn.addEventListener('click', toggleAutoScroll);
  DOM.speedSlider.addEventListener('input', (e) => {
    state.autoScroll.speed = parseFloat(e.target.value);
    updateSpeedLabel();
  });
  DOM.speedUpBtn.addEventListener('click', () => {
    let s = Math.min(5.0, state.autoScroll.speed + 0.25);
    state.autoScroll.speed = s;
    DOM.speedSlider.value = s;
    updateSpeedLabel();
  });
  DOM.speedDownBtn.addEventListener('click', () => {
    let s = Math.max(0.5, state.autoScroll.speed - 0.25);
    state.autoScroll.speed = s;
    DOM.speedSlider.value = s;
    updateSpeedLabel();
  });

  // التوقف المؤقت الذكي عند تفاعل المستخدم أثناء الحركة التلقائية
  const notifyUserInteraction = () => {
    if (!state.autoScroll.isActive) return;
    state.autoScroll.userInteracting = true;
    clearTimeout(state.autoScroll.interactionTimeout);
    state.autoScroll.interactionTimeout = setTimeout(() => {
      state.autoScroll.userInteracting = false;
    }, 1200);
  };

  window.addEventListener('wheel', notifyUserInteraction, { passive: true });
  window.addEventListener('touchmove', notifyUserInteraction, { passive: true });
  window.addEventListener('mousedown', notifyUserInteraction, { passive: true });

  // دعم السحب باللمس للتقليب الجانبي (Touch Gestures)
  let touchStartX = 0;
  let touchEndX = 0;
  DOM.quranTextContainer.addEventListener('touchstart', (e) => {
    touchStartX = e.changedTouches[0].screenX;
  }, { passive: true });

  DOM.quranTextContainer.addEventListener('touchend', (e) => {
    touchEndX = e.changedTouches[0].screenX;
    handleSwipeGesture();
  }, { passive: true });

  function handleSwipeGesture() {
    if (state.currentMode !== 'horizontal') return;
    const threshold = 60;
    const diff = touchEndX - touchStartX;
    if (diff > threshold) {
      // سحب لليمين: السابق
      if (state.currentSurahId > 1) loadSurah(state.currentSurahId - 1);
    } else if (diff < -threshold) {
      // سحب لليسار: التالي
      if (state.currentSurahId < 114) loadSurah(state.currentSurahId + 1);
    }
  }

  // دعم الأسهم من لوحة المفاتيح
  window.addEventListener('keydown', (e) => {
    if (state.currentMode === 'horizontal') {
      if (e.key === 'ArrowLeft' && state.currentSurahId < 114) {
        loadSurah(state.currentSurahId + 1);
      } else if (e.key === 'ArrowRight' && state.currentSurahId > 1) {
        loadSurah(state.currentSurahId - 1);
      }
    } else if (state.currentMode === 'vertical') {
      if (e.key === ' ') {
        e.preventDefault();
        toggleAutoScroll();
      }
    }
  });

  // مشغل الصوت
  DOM.audioPlayPauseBtn.addEventListener('click', toggleAudio);
  DOM.stopAudioBtn.addEventListener('click', stopAudio);

  // فتح وإغلاق النوافذ والقوائم
  DOM.openIndexBtn.addEventListener('click', () => openDrawer(DOM.surahIndexDrawer));
  DOM.openThemesBtn.addEventListener('click', () => openDrawer(DOM.themesDrawer));
  DOM.openSettingsBtn.addEventListener('click', () => openDrawer(DOM.settingsDrawer));
  DOM.openBookmarksBtn.addEventListener('click', () => openDrawer(DOM.bookmarksDrawer));
  DOM.openDuaaBtn.addEventListener('click', () => openDrawer(DOM.duaaDrawer));
  DOM.drawerBackdrop.addEventListener('click', closeAllDrawers);

  document.querySelectorAll('.close-modal-btn').forEach(btn => {
    btn.addEventListener('click', closeAllDrawers);
  });

  // البحث في السور
  DOM.surahSearchInput.addEventListener('input', (e) => {
    filterSurahIndex(e.target.value);
  });

  // اختيار الثيمات
  document.querySelectorAll('.theme-card').forEach(card => {
    card.addEventListener('click', () => {
      applyTheme(card.dataset.setTheme);
    });
  });

  // تعديل الخط
  DOM.fontSizeSlider.addEventListener('input', (e) => {
    applyFontSize(parseInt(e.target.value, 10));
  });
  DOM.fontIncBtn.addEventListener('click', () => {
    applyFontSize(Math.min(52, state.fontSize + 2));
  });
  DOM.fontDecBtn.addEventListener('click', () => {
    applyFontSize(Math.max(20, state.fontSize - 2));
  });

  DOM.fontFamilySelect.addEventListener('change', (e) => {
    state.fontFamily = e.target.value;
    document.documentElement.style.setProperty('--font-quran', state.fontFamily);
  });

  // حفظ الفاصلة
  DOM.saveCurrentBookmarkBtn.addEventListener('click', saveCurrentBookmark);

  // وضع ملء الشاشة
  DOM.toggleFullscreenBtn.addEventListener('click', () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  });

  // فحص تدوير الشاشة
  window.addEventListener('resize', checkOrientation);
  window.addEventListener('orientationchange', checkOrientation);
}

function openDrawer(drawerElement) {
  closeAllDrawers();
  drawerElement.classList.add('open');
  DOM.drawerBackdrop.classList.add('open');
}

function closeAllDrawers() {
  document.querySelectorAll('.side-drawer').forEach(d => d.classList.remove('open'));
  DOM.tafsirSheet.classList.remove('open');
  DOM.drawerBackdrop.classList.remove('open');
}

// ==========================================================================
// نظام التحديث التلقائي المباشر (Auto-Updater via GitHub) و Service Worker
// ==========================================================================

// تسجيل الـ Service Worker لتثبيت التطبيق والعمل بدون إنترنت
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js')
      .then((reg) => {
        // التحقق من وجود تحديثات للملفات عند فتح البرنامج
        reg.onupdatefound = () => {
          const installingWorker = reg.installing;
          installingWorker.onstatechange = () => {
            if (installingWorker.state === 'installed' && navigator.serviceWorker.controller) {
              showUpdateToast({
                version: "أحدث",
                releaseNotes: "تم تحميل ملفات وتحديثات جديدة للبرنامج جاهزة للتطبيق."
              });
            }
          };
        };
      })
      .catch((err) => console.log('ServiceWorker registration failed:', err));
  });
}

// دالة فحص الإصدار من GitHub
async function checkForAppUpdates(isManual = false) {
  try {
    // جلب ملف version.json مع منع التخزين المؤقت
    const response = await fetch(`./version.json?t=${Date.now()}`);
    if (response.ok) {
      const data = await response.json();
      if (data && data.version && isNewerVersion(data.version, CURRENT_APP_VERSION)) {
        showUpdateToast(data);
        return;
      }
    }
    if (isManual) {
      alert(`برنامج «تلاوتي» محدث بالكامل!\nأنت تستخدم آخر إصدار متوفر (v${CURRENT_APP_VERSION}).`);
    }
  } catch (err) {
    if (isManual) {
      alert("تعذر الاتصال للتحقق من التحديثات. تأكد من اتصالك بالإنترنت.");
    }
  }
}

// مقارنة أرقام الإصدارات (مثلاً: 1.1.0 أكبر من 1.0.0)
function isNewerVersion(remote, local) {
  const rParts = remote.split('.').map(Number);
  const lParts = local.split('.').map(Number);
  for (let i = 0; i < Math.max(rParts.length, lParts.length); i++) {
    const r = rParts[i] || 0;
    const l = lParts[i] || 0;
    if (r > l) return true;
    if (r < l) return false;
  }
  return false;
}

// إظهار إشعار التحديث داخل التطبيق
function showUpdateToast(updateInfo) {
  const toast = document.getElementById('updateToast');
  const title = document.getElementById('updateVersionTitle');
  const notes = document.getElementById('updateNotesText');
  const applyBtn = document.getElementById('applyUpdateBtn');
  const dismissBtn = document.getElementById('dismissUpdateBtn');

  title.textContent = `يتوفر تحديث جديد: v${updateInfo.version}`;
  notes.textContent = updateInfo.releaseNotes || 'يحتوي هذا التحديث على تحسينات وإضافات جديدة لتجربة قراءة أفضل.';
  toast.style.display = 'block';

  applyBtn.onclick = async () => {
    applyBtn.textContent = 'جاري التحديث... ⏳';
    applyBtn.disabled = true;

    // مسح الكاش بالكامل وتحميل النسخة الجديدة فوراً
    if ('caches' in window) {
      const cacheNames = await caches.keys();
      await Promise.all(cacheNames.map(name => caches.delete(name)));
    }

    // إعادة تحميل الصفحة لتطبيق التحديث
    setTimeout(() => {
      window.location.reload(true);
    }, 600);
  };

  dismissBtn.onclick = () => {
    toast.style.display = 'none';
  };
}

// ربط زر الفحص اليدوي في شاشة الإعدادات
const manualCheckBtn = document.getElementById('manualCheckUpdateBtn');
if (manualCheckBtn) {
  manualCheckBtn.addEventListener('click', () => checkForAppUpdates(true));
}

// فحص التحديثات تلقائياً في الخلفية بعد 3 ثوانٍ من تشغيل البرنامج
setTimeout(() => {
  checkForAppUpdates(false);
}, 3000);


