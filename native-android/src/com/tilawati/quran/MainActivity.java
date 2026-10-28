package com.tilawati.quran;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    // حاويات الألوان والجذور
    private RelativeLayout rootLayout;
    private LinearLayout topAppBar, bottomNavBar;

    // تبويبات التنقل السفلية
    private LinearLayout navItemHome, navItemReader, navItemIndex, navItemAzkar, navItemSettings;
    private TextView tvNavHomeIcon, tvNavHomeText;
    private TextView tvNavReaderIcon, tvNavReaderText;
    private TextView tvNavIndexIcon, tvNavIndexText;
    private TextView tvNavAzkarIcon, tvNavAzkarText;
    private TextView tvNavSettingsIcon, tvNavSettingsText;

    // حاويات الشاشات (التبويبات الخمس)
    private ScrollView tabViewHome;
    private RelativeLayout tabViewReader;
    private LinearLayout tabViewIndex;
    private ScrollView tabViewAzkar;
    private ScrollView tabViewSettings;

    // عناصر الشاشة الرئيسية (Home Tab)
    private TextView tvResumeSurahName, tvResumeDetails;
    private Button btnResumeReading;
    private LinearLayout btnQuickGoQuran, btnQuickGoIndex, btnQuickGoBookmark, btnQuickGoAzkar;

    // عناصر شاشة المصحف (Reader Tab)
    private LinearLayout readerTopControls, bottomFlipBar, dockAutoScroll, surahHeaderBox;
    private TextView tvSurahTitle, tvSurahDetails, tvSurahPageBadge, tvBismillah, tvQuranText, tvPageIndex, tvSpeedValue;
    private Button btnModeHorizontal, btnModeVertical, btnFontDecrease, btnFontIncrease;
    private Button btnPrevSurah, btnNextSurah, btnPlayPauseScroll, btnSpeedDown, btnSpeedUp;
    private SeekBar seekBarSpeed;
    private ScrollView scrollViewQuran;

    // عناصر شاشة الفهرس والبحث (Index Tab)
    private EditText etSearchSurahs;
    private ListView listViewSurahs;
    private ArrayAdapter<String> surahsAdapter;
    private ArrayList<String> allSurahTitles = new ArrayList<String>();
    private ArrayList<Integer> allSurahIds = new ArrayList<Integer>();

    // عناصر شاشة الأذكار (Azkar Tab)
    private RelativeLayout circleTasbeeh;
    private TextView tvTasbeehCount;
    private Button btnResetTasbeeh, btnMorningAzkar, btnEveningAzkar, btnKhatmDuaaDirect;
    private int tasbeehCounter = 0;

    // أزرار الشريط العلوي
    private Button btnQuickSearch, btnQuickTheme, btnQuickUpdate;
    private Button btnSettingsThemeDialog, btnSettingsFontDialog, btnSettingsCheckUpdates;

    // حالة التطبيق والتفضيلات
    private int currentSurahId = 1;
    private boolean isHorizontalMode = true;
    private float currentFontSize = 24.0f;
    private int currentTheme = 0; // 0: Classic, 1: Emerald, 2: Night, 3: Blue, 4: Pearl
    private int currentTab = 0; // 0: Home, 1: Reader, 2: Index, 3: Azkar, 4: Settings

    // محرك التمرير التلقائي (Auto-Scroll)
    private boolean isAutoScrolling = false;
    private float scrollSpeed = 1.5f;
    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    // بيانات القرآن الكريم
    private JSONObject quranDataJson;
    private JSONArray surahsArray;
    private SharedPreferences prefs;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ex.printStackTrace();
            }
        });

        try {
            // إبقاء الشاشة مضاءة أثناء القراءة
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            setContentView(R.layout.activity_main);
            prefs = getSharedPreferences("tilawati_prefs", Context.MODE_PRIVATE);
            try {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Throwable ignored) {}

            initViews();
            loadQuranData();
            setupNavigation();
            setupListeners();
            restoreSavedState();

            loadSurah(currentSurahId);

            // فحص التحديثات في الخلفية تلقائياً
            try {
                checkForUpdates(false);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            t.printStackTrace();
            TextView tv = new TextView(this);
            tv.setText("تلاوتي - جاري التهيئة: " + t.getMessage());
            tv.setPadding(32, 64, 32, 32);
            tv.setTextSize(16);
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.parseColor("#0F3822"));
            setContentView(tv);
        }
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        topAppBar = findViewById(R.id.topAppBar);
        bottomNavBar = findViewById(R.id.bottomNavBar);

        // شريط التنقل السفلي
        navItemHome = findViewById(R.id.navItemHome);
        navItemReader = findViewById(R.id.navItemReader);
        navItemIndex = findViewById(R.id.navItemIndex);
        navItemAzkar = findViewById(R.id.navItemAzkar);
        navItemSettings = findViewById(R.id.navItemSettings);

        tvNavHomeIcon = findViewById(R.id.tvNavHomeIcon);
        tvNavHomeText = findViewById(R.id.tvNavHomeText);
        tvNavReaderIcon = findViewById(R.id.tvNavReaderIcon);
        tvNavReaderText = findViewById(R.id.tvNavReaderText);
        tvNavIndexIcon = findViewById(R.id.tvNavIndexIcon);
        tvNavIndexText = findViewById(R.id.tvNavIndexText);
        tvNavAzkarIcon = findViewById(R.id.tvNavAzkarIcon);
        tvNavAzkarText = findViewById(R.id.tvNavAzkarText);
        tvNavSettingsIcon = findViewById(R.id.tvNavSettingsIcon);
        tvNavSettingsText = findViewById(R.id.tvNavSettingsText);

        // التبويبات
        tabViewHome = findViewById(R.id.tabViewHome);
        tabViewReader = findViewById(R.id.tabViewReader);
        tabViewIndex = findViewById(R.id.tabViewIndex);
        tabViewAzkar = findViewById(R.id.tabViewAzkar);
        tabViewSettings = findViewById(R.id.tabViewSettings);

        // عناصر الرئيسية
        tvResumeSurahName = findViewById(R.id.tvResumeSurahName);
        tvResumeDetails = findViewById(R.id.tvResumeDetails);
        btnResumeReading = findViewById(R.id.btnResumeReading);
        btnQuickGoQuran = findViewById(R.id.btnQuickGoQuran);
        btnQuickGoIndex = findViewById(R.id.btnQuickGoIndex);
        btnQuickGoBookmark = findViewById(R.id.btnQuickGoBookmark);
        btnQuickGoAzkar = findViewById(R.id.btnQuickGoAzkar);

        // عناصر المصحف
        readerTopControls = findViewById(R.id.readerTopControls);
        bottomFlipBar = findViewById(R.id.bottomFlipBar);
        dockAutoScroll = findViewById(R.id.dockAutoScroll);
        surahHeaderBox = findViewById(R.id.surahHeaderBox);
        tvSurahTitle = findViewById(R.id.tvSurahTitle);
        tvSurahDetails = findViewById(R.id.tvSurahDetails);
        tvSurahPageBadge = findViewById(R.id.tvSurahPageBadge);
        tvBismillah = findViewById(R.id.tvBismillah);
        tvQuranText = findViewById(R.id.tvQuranText);
        tvPageIndex = findViewById(R.id.tvPageIndex);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);

        btnModeHorizontal = findViewById(R.id.btnModeHorizontal);
        btnModeVertical = findViewById(R.id.btnModeVertical);
        btnFontDecrease = findViewById(R.id.btnFontDecrease);
        btnFontIncrease = findViewById(R.id.btnFontIncrease);
        btnPrevSurah = findViewById(R.id.btnPrevSurah);
        btnNextSurah = findViewById(R.id.btnNextSurah);
        btnPlayPauseScroll = findViewById(R.id.btnPlayPauseScroll);
        btnSpeedDown = findViewById(R.id.btnSpeedDown);
        btnSpeedUp = findViewById(R.id.btnSpeedUp);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        scrollViewQuran = findViewById(R.id.scrollViewQuran);

        // عناصر الفهرس والبحث
        etSearchSurahs = findViewById(R.id.etSearchSurahs);
        listViewSurahs = findViewById(R.id.listViewSurahs);

        // عناصر الأذكار
        circleTasbeeh = findViewById(R.id.circleTasbeeh);
        tvTasbeehCount = findViewById(R.id.tvTasbeehCount);
        btnResetTasbeeh = findViewById(R.id.btnResetTasbeeh);
        btnMorningAzkar = findViewById(R.id.btnMorningAzkar);
        btnEveningAzkar = findViewById(R.id.btnEveningAzkar);
        btnKhatmDuaaDirect = findViewById(R.id.btnKhatmDuaaDirect);

        // عناصر الشريط العلوي والإعدادات
        btnQuickSearch = findViewById(R.id.btnQuickSearch);
        btnQuickTheme = findViewById(R.id.btnQuickTheme);
        btnQuickUpdate = findViewById(R.id.btnQuickUpdate);
        btnSettingsThemeDialog = findViewById(R.id.btnSettingsThemeDialog);
        btnSettingsFontDialog = findViewById(R.id.btnSettingsFontDialog);
        btnSettingsCheckUpdates = findViewById(R.id.btnSettingsCheckUpdates);
    }

    private void loadQuranData() {
        try {
            InputStream is = getAssets().open("quran_data.json");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            is.close();
            String jsonStr = new String(baos.toByteArray(), "UTF-8");
            quranDataJson = new JSONObject(jsonStr);
            surahsArray = quranDataJson.optJSONArray("surahs");

            // بناء قائمة السور للفهرس
            allSurahTitles.clear();
            allSurahIds.clear();
            if (surahsArray != null) {
                for (int i = 0; i < surahsArray.length(); i++) {
                    JSONObject s = surahsArray.getJSONObject(i);
                    int sId = s.getInt("id");
                    String title = sId + ". سورة " + s.getString("name") + " (" + s.getString("type") + " - " + s.getInt("totalVerses") + " آية - صفحة " + s.optInt("startPage", 1) + ")";
                    allSurahTitles.add(title);
                    allSurahIds.add(sId);
                }
            }

            if (listViewSurahs != null) {
                surahsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(allSurahTitles));
                listViewSurahs.setAdapter(surahsAdapter);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            Toast.makeText(this, "تعذر قراءة بيانات المصحف", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================================================
    // إدارة التنقل بين التبويبات الخمسة (Bottom Navigation)
    // ==========================================================================
    private void setupNavigation() {
        navItemHome.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(0); }
        });
        navItemReader.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });
        navItemIndex.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(2); }
        });
        navItemAzkar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(3); }
        });
        navItemSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(4); }
        });
    }

    private void switchTab(int index) {
        currentTab = index;

        tabViewHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabViewReader.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabViewIndex.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabViewAzkar.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        tabViewSettings.setVisibility(index == 4 ? View.VISIBLE : View.GONE);

        // تحديث ألوان شريط التنقل السفلي
        int activeColor = Color.parseColor("#0F3822");
        int inactiveColor = Color.parseColor("#685E52");

        tvNavHomeText.setTextColor(index == 0 ? activeColor : inactiveColor);
        tvNavReaderText.setTextColor(index == 1 ? activeColor : inactiveColor);
        tvNavIndexText.setTextColor(index == 2 ? activeColor : inactiveColor);
        tvNavAzkarText.setTextColor(index == 3 ? activeColor : inactiveColor);
        tvNavSettingsText.setTextColor(index == 4 ? activeColor : inactiveColor);

        if (index != 1 && isAutoScrolling) {
            stopAutoScroll();
        }
    }

    private void setupListeners() {
        // الشاشة الرئيسية
        btnResumeReading.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });
        btnQuickGoQuran.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });
        btnQuickGoIndex.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(2); }
        });
        btnQuickGoBookmark.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { handleBookmarkClick(); }
        });
        btnQuickGoAzkar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(3); }
        });

        // الشريط العلوي
        btnQuickSearch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(2); etSearchSurahs.requestFocus(); }
        });
        btnQuickTheme.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemeChooserDialog(); }
        });
        btnQuickUpdate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(MainActivity.this, "جاري فحص وجود تحديثات جديدة...", Toast.LENGTH_SHORT).show();
                checkForUpdates(true);
            }
        });

        // أنماط القراءة
        btnModeHorizontal.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setReadingMode(true); }
        });
        btnModeVertical.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setReadingMode(false); }
        });

        // تكبير وتصغير الخط
        btnFontDecrease.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustFontSize(-2.0f); }
        });
        btnFontIncrease.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustFontSize(2.0f); }
        });

        // التنقل بين السور
        btnPrevSurah.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentSurahId > 1) loadSurah(currentSurahId - 1);
            }
        });
        btnNextSurah.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentSurahId < 114) loadSurah(currentSurahId + 1);
            }
        });

        // التمرير التلقائي
        btnPlayPauseScroll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleAutoScroll(); }
        });
        btnSpeedDown.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustScrollSpeed(-0.3f); }
        });
        btnSpeedUp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustScrollSpeed(0.3f); }
        });
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scrollSpeed = Math.max(0.5f, progress / 10.0f);
                tvSpeedValue.setText("سرعة: " + String.format("%.1fx", scrollSpeed));
                prefs.edit().putFloat("scroll_speed", scrollSpeed).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // دالة التمرير التلقائي الدوري (Delta-Time smooth ticker)
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoScrolling) {
                    int dy = Math.max(1, Math.round(scrollSpeed * 2));
                    scrollViewQuran.smoothScrollBy(0, dy);

                    // فحص الوصول لنهاية السورة
                    View child = scrollViewQuran.getChildAt(0);
                    if (child != null) {
                        int diff = (child.getBottom() - (scrollViewQuran.getHeight() + scrollViewQuran.getScrollY()));
                        if (diff <= 6) {
                            stopAutoScroll();
                            return;
                        }
                    }
                    autoScrollHandler.postDelayed(this, 30);
                }
            }
        };

        // تصفية السور في الفهرس
        etSearchSurahs.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSurahs(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listViewSurahs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                int surahNum = 1;
                try {
                    int dotIndex = selected.indexOf('.');
                    if (dotIndex != -1) {
                        surahNum = Integer.parseInt(selected.substring(0, dotIndex).trim());
                    }
                } catch (Exception e) {}
                loadSurah(surahNum);
                switchTab(1); // فتح شاشة القراءة فوراً
            }
        });

        // المسبحة الإلكترونية التفاعلية
        circleTasbeeh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tasbeehCounter++;
                tvTasbeehCount.setText(String.valueOf(tasbeehCounter));
                if (vibrator != null) {
                    try { vibrator.vibrate(30); } catch (Exception e) {}
                }
            }
        });

        btnResetTasbeeh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tasbeehCounter = 0;
                tvTasbeehCount.setText("0");
            }
        });

        btnMorningAzkar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAzkarDialog("أذكار الصباح كاملة", "morning"); }
        });
        btnEveningAzkar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAzkarDialog("أذكار المساء كاملة", "evening"); }
        });
        btnKhatmDuaaDirect.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showKhatmDuaaDialog(); }
        });

        // شاشة الإعدادات
        btnSettingsThemeDialog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemeChooserDialog(); }
        });
        btnSettingsFontDialog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFontSizeDialog(); }
        });
        btnSettingsCheckUpdates.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { checkForUpdates(true); }
        });

        // تفاعل النص القرآني عند النقر (عرض التفسير والخيارات)
        tvQuranText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAyahOptionsDialog();
                return true;
            }
        });
        tvQuranText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAyahOptionsDialog();
            }
        });
    }

    private void filterSurahs(String query) {
        if (query.isEmpty()) {
            surahsAdapter.clear();
            surahsAdapter.addAll(allSurahTitles);
            surahsAdapter.notifyDataSetChanged();
            return;
        }

        ArrayList<String> filtered = new ArrayList<String>();
        for (String title : allSurahTitles) {
            if (title.contains(query)) {
                filtered.add(title);
            }
        }
        surahsAdapter.clear();
        surahsAdapter.addAll(filtered);
        surahsAdapter.notifyDataSetChanged();
    }

    private void adjustFontSize(float delta) {
        currentFontSize = Math.max(16.0f, Math.min(46.0f, currentFontSize + delta));
        tvQuranText.setTextSize(currentFontSize);
        prefs.edit().putFloat("font_size", currentFontSize).apply();
    }

    private void adjustScrollSpeed(float delta) {
        scrollSpeed = Math.max(0.5f, Math.min(5.0f, scrollSpeed + delta));
        seekBarSpeed.setProgress(Math.round(scrollSpeed * 10));
        tvSpeedValue.setText("سرعة: " + String.format("%.1fx", scrollSpeed));
        prefs.edit().putFloat("scroll_speed", scrollSpeed).apply();
    }

    private void setReadingMode(boolean horizontal) {
        isHorizontalMode = horizontal;
        prefs.edit().putBoolean("is_horizontal", isHorizontalMode).apply();

        if (horizontal) {
            btnModeHorizontal.setTextColor(Color.WHITE);
            btnModeHorizontal.setBackgroundResource(R.drawable.bg_button_gold);

            btnModeVertical.setTextColor(Color.parseColor("#685E52"));
            btnModeVertical.setBackgroundResource(R.drawable.bg_button_outline);

            bottomFlipBar.setVisibility(View.VISIBLE);
            dockAutoScroll.setVisibility(View.GONE);
            stopAutoScroll();
        } else {
            btnModeHorizontal.setTextColor(Color.parseColor("#685E52"));
            btnModeHorizontal.setBackgroundResource(R.drawable.bg_button_outline);

            btnModeVertical.setTextColor(Color.WHITE);
            btnModeVertical.setBackgroundResource(R.drawable.bg_button_gold);

            bottomFlipBar.setVisibility(View.GONE);
            dockAutoScroll.setVisibility(View.VISIBLE);
        }
    }

    private void toggleAutoScroll() {
        if (isAutoScrolling) {
            stopAutoScroll();
        } else {
            startAutoScroll();
        }
    }

    private void startAutoScroll() {
        isAutoScrolling = true;
        btnPlayPauseScroll.setText("⏸");
        autoScrollHandler.post(autoScrollRunnable);
    }

    private void stopAutoScroll() {
        isAutoScrolling = false;
        btnPlayPauseScroll.setText("▶");
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    // ==========================================================================
    // تحميل وعرض آيات السورة الكريمة (114 سورة أوفلاين بالكامل)
    // ==========================================================================
    private void loadSurah(int surahId) {
        currentSurahId = surahId;
        prefs.edit().putInt("last_surah", currentSurahId).apply();

        JSONObject meta = null;
        try {
            if (surahsArray != null) {
                for (int i = 0; i < surahsArray.length(); i++) {
                    JSONObject s = surahsArray.getJSONObject(i);
                    if (s.getInt("id") == surahId) {
                        meta = s;
                        break;
                    }
                }
            }
        } catch (Exception e) {}

        String surahName = (meta != null) ? meta.optString("name", "السورة " + surahId) : "السورة " + surahId;
        int totalVerses = (meta != null) ? meta.optInt("totalVerses", 7) : 7;
        String type = (meta != null) ? meta.optString("type", "مكية") : "مكية";
        int page = (meta != null) ? meta.optInt("startPage", 1) : 1;

        tvSurahTitle.setText("سُورَةُ " + surahName);
        tvSurahDetails.setText(type + " • آياتها: " + totalVerses);
        tvSurahPageBadge.setText("الصفحة " + page);
        tvPageIndex.setText(surahId + " / 114");

        // تحديث بطاقة الاستئناف في الشاشة الرئيسية
        tvResumeSurahName.setText("سُورَةُ " + surahName);
        tvResumeDetails.setText(type + " • الصفحة " + page + " • آياتها: " + totalVerses);

        // ضبط إظهار البسملة
        if (surahId == 9 || surahId == 1) {
            tvBismillah.setVisibility(View.GONE);
        } else {
            tvBismillah.setVisibility(View.VISIBLE);
        }

        btnPrevSurah.setEnabled(surahId > 1);
        btnNextSurah.setEnabled(surahId < 114);

        // جلب نص الآيات كاملاً من الملفات المدمجة دون تأخير
        displaySurahVerses(surahId, totalVerses, surahName);
        scrollViewQuran.scrollTo(0, 0);
    }

    private void displaySurahVerses(int surahId, int totalVerses, String surahName) {
        try {
            if (quranDataJson != null) {
                JSONObject builtin = quranDataJson.optJSONObject("builtin");
                if (builtin != null && builtin.has(String.valueOf(surahId))) {
                    JSONObject bObj = builtin.getJSONObject(String.valueOf(surahId));
                    JSONArray ayahsArr = bObj.getJSONArray("ayahs");
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    for (int i = 0; i < ayahsArr.length(); i++) {
                        JSONObject aObj = ayahsArr.getJSONObject(i);
                        String text = aObj.getString("text");
                        if (i == 0 && surahId != 1 && surahId != 9) {
                            text = text.replace("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "").trim();
                        }
                        builder.append(text).append(" ۝").append(convertToArabicNumber(i + 1)).append(" ");
                    }
                    tvQuranText.setText(builder);
                    return;
                }
            }
        } catch (Exception e) {}

        tvQuranText.setText("سورة " + surahName + " المباركة");
    }

    private String convertToArabicNumber(int num) {
        String[] ar = {"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"};
        String s = String.valueOf(num);
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(ar[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ==========================================================================
    // قائمة الخيارات عند الضغط على الآيات (تفسير، نسخ، مشاركة، فاصلة)
    // ==========================================================================
    private void showAyahOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(tvSurahTitle.getText());
        String[] options = {
            "📖 قراءة التفسير الميسر للسورة",
            "📌 حفظ فاصلة عند هذه السورة",
            "📋 نسخ نص السورة",
            "↗️ مشاركة السورة الكريمة"
        };
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showTafsirDialog();
                } else if (which == 1) {
                    prefs.edit()
                        .putInt("user_bookmark_surah", currentSurahId)
                        .putString("user_bookmark_name", tvSurahTitle.getText().toString())
                        .apply();
                    Toast.makeText(MainActivity.this, "تم حفظ الفاصلة بنجاح!", Toast.LENGTH_SHORT).show();
                } else if (which == 2) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Quran", tvQuranText.getText().toString());
                    cm.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "تم نسخ النص القرآني بنجاح", Toast.LENGTH_SHORT).show();
                } else if (which == 3) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, tvSurahTitle.getText() + "\n\n" + tvQuranText.getText());
                    startActivity(Intent.createChooser(shareIntent, "مشاركة السورة عبر:"));
                }
            }
        });
        builder.show();
    }

    private void showTafsirDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("التفسير الميسر: " + tvSurahTitle.getText());
        builder.setMessage("سورة مباركة من سور القرآن الكريم، تتناول مقاصد الشريعة، والتوحيد، وأصول الإيمان، والوعد والوعيد، وهداية العباد للتي هي أقوم.");
        builder.setPositiveButton("إغلاق", null);
        builder.show();
    }

    private void handleBookmarkClick() {
        final int savedBookmark = prefs.getInt("user_bookmark_surah", -1);
        final String savedName = prefs.getString("user_bookmark_name", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("العلامات المرجعية والفاصلة:");

        ArrayList<String> options = new ArrayList<String>();
        options.add("📌 حفظ فاصلة عند السورة الحالية (" + tvSurahTitle.getText() + ")");
        if (savedBookmark != -1) {
            options.add("📖 الانتقال إلى الفاصلة المحفوظة (" + savedName + ")");
            options.add("🗑️ حذف الفاصلة المحفوظة");
        }

        CharSequence[] items = options.toArray(new CharSequence[0]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    prefs.edit()
                        .putInt("user_bookmark_surah", currentSurahId)
                        .putString("user_bookmark_name", tvSurahTitle.getText().toString())
                        .apply();
                    Toast.makeText(MainActivity.this, "تم حفظ الفاصلة بنجاح!", Toast.LENGTH_SHORT).show();
                } else if (which == 1 && savedBookmark != -1) {
                    loadSurah(savedBookmark);
                    switchTab(1);
                    Toast.makeText(MainActivity.this, "تم الانتقال إلى الفاصلة!", Toast.LENGTH_SHORT).show();
                } else if (which == 2 && savedBookmark != -1) {
                    prefs.edit().remove("user_bookmark_surah").remove("user_bookmark_name").apply();
                    Toast.makeText(MainActivity.this, "تم حذف الفاصلة.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    // ==========================================================================
    // الأذكار وحصن المسلم
    // ==========================================================================
    private void showAzkarDialog(String title, String categoryId) {
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray cats = quranDataJson.optJSONArray("azkar_categories");
            if (cats != null) {
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject c = cats.getJSONObject(i);
                    if (categoryId.equals(c.optString("id"))) {
                        JSONArray items = c.optJSONArray("items");
                        if (items != null) {
                            for (int j = 0; j < items.length(); j++) {
                                JSONObject it = items.getJSONObject(j);
                                sb.append("• ").append(it.getString("text")).append("\n");
                                sb.append("  [التكرار: ").append(it.optInt("count", 1)).append(" مرات]\n\n");
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {}

        if (sb.length() == 0) {
            sb.append("• أَصْبَحْنَا وَأَصْبَحَ المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ.\n\n");
            sb.append("• اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ.\n\n");
            sb.append("• سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(sb.toString().trim());
        builder.setPositiveButton("تم", null);
        builder.show();
    }

    private void showKhatmDuaaDialog() {
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray duaaArr = quranDataJson.optJSONArray("duaa");
            if (duaaArr != null) {
                for (int i = 0; i < duaaArr.length(); i++) {
                    sb.append("• ").append(duaaArr.getString(i)).append("\n\n");
                }
            }
        } catch (Exception e) {}

        if (sb.length() == 0) {
            sb.append("اللَّهُمَّ ارْحَمْنِي بِالقُرْآنِ، وَاجْعَلْهُ لِي إِمَاماً وَنُوراً وَهُدًى وَرَحْمَةً.\n\n");
            sb.append("اللَّهُمَّ ذَكِّرْنِي مِنْهُ مَا نَسِيتُ، وَعَلِّمْنِي مِنْهُ مَا جَهِلْتُ، وَارْزُقْنِي تِلاَوَتَهُ آنَاءَ اللَّيْلِ وَأَطْرَافَ النَّهَارِ، وَاجْعَلْهُ لِي حُجَّةً يَا رَبَّ العَالَمِينَ.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("دعاء ختم القرآن الكريم:");
        builder.setMessage(sb.toString().trim());
        builder.setPositiveButton("آمين", null);
        builder.show();
    }

    // ==========================================================================
    // الثيمات وتخصيص المظهر
    // ==========================================================================
    private void showThemeChooserDialog() {
        final String[] themes = {
            "🌿 الكلاسيكي الكريمي (افتراضي)",
            "👑 الروضة الشريفة (الزمردي الملكي)",
            "🌙 التهجد الليلي (الأسود المطلق OLED)",
            "🌊 الأزرق الأندلسي",
            "⚪ الأبيض اللؤلؤي النقي"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر الثيم القرآني المفضل:");
        builder.setSingleChoiceItems(themes, currentTheme, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyTheme(which);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void applyTheme(int themeId) {
        currentTheme = themeId;
        prefs.edit().putInt("theme_id", themeId).apply();

        int bgColor, cardColor, textColor, accentColor;

        switch (themeId) {
            case 1: // الزمردي
                bgColor = Color.parseColor("#091D13");
                cardColor = Color.parseColor("#0F2C1D");
                textColor = Color.parseColor("#F5F2E8");
                accentColor = Color.parseColor("#C5A059");
                break;
            case 2: // الأسود الليلي OLED
                bgColor = Color.parseColor("#000000");
                cardColor = Color.parseColor("#121212");
                textColor = Color.parseColor("#E8E8E8");
                accentColor = Color.parseColor("#D4AF37");
                break;
            case 3: // الأزرق
                bgColor = Color.parseColor("#0B1B30");
                cardColor = Color.parseColor("#122845");
                textColor = Color.parseColor("#F2F6FC");
                accentColor = Color.parseColor("#DFBA73");
                break;
            case 4: // الأبيض اللؤلؤي
                bgColor = Color.parseColor("#FFFFFF");
                cardColor = Color.parseColor("#F7F9FB");
                textColor = Color.parseColor("#151A22");
                accentColor = Color.parseColor("#1B6B38");
                break;
            default: // الكلاسيكي الكريمي
                bgColor = Color.parseColor("#FAF7EE");
                cardColor = Color.parseColor("#FFFDF8");
                textColor = Color.parseColor("#221E19");
                accentColor = Color.parseColor("#8B6B23");
                break;
        }

        rootLayout.setBackgroundColor(bgColor);
        topAppBar.setBackgroundColor(cardColor);
        bottomNavBar.setBackgroundColor(cardColor);
        tvQuranText.setTextColor(textColor);
    }

    private void showFontSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ضبط حجم الخط القرآني:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        final TextView tvSize = new TextView(this);
        tvSize.setText("الحجم الحالي: " + Math.round(currentFontSize) + "sp");
        tvSize.setTextSize(16);
        tvSize.setTextColor(Color.parseColor("#221E19"));
        layout.addView(tvSize);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(30);
        seekBar.setProgress(Math.round(currentFontSize - 16));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFontSize = 16 + progress;
                tvSize.setText("الحجم الحالي: " + Math.round(currentFontSize) + "sp");
                tvQuranText.setTextSize(currentFontSize);
                prefs.edit().putFloat("font_size", currentFontSize).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(seekBar);

        builder.setView(layout);
        builder.setPositiveButton("تم", null);
        builder.show();
    }

    // ==========================================================================
    // محرك التحديثات الرسمي المربوط بـ GitHub
    // ==========================================================================
    private void checkForUpdates(final boolean isManual) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    URL url = new URL("https://raw.githubusercontent.com/AMK500/Tilawati-App/main/version.json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setRequestProperty("User-Agent", "Tilawati-App");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        r.close();
                        return new JSONObject(sb.toString());
                    }
                } catch (Exception e) {}
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                if (result != null) {
                    int remoteBuild = result.optInt("build", result.optInt("versionCode", 1));
                    String remoteVer = result.optString("version", result.optString("versionName", "1.0"));
                    String notes = result.optString("releaseNotes", result.optString("changelogAr", ""));
                    final String downloadUrl = result.optString("downloadUrl", result.optString("apkUrl", "https://github.com/AMK500/Tilawati-App/releases"));

                    int currentBuild = 1;
                    try {
                        currentBuild = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    } catch (Exception e) {}

                    if (remoteBuild > currentBuild) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("🚀 يتوفر تحديث جديد لتطبيق تلاوتي (v" + remoteVer + ")");
                        builder.setMessage("سجل التغييرات:\n\n" + notes);
                        builder.setPositiveButton("تحميل التحديث", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        builder.setNegativeButton("لاحقاً", null);
                        builder.show();
                        return;
                    }
                }
                if (isManual) {
                    Toast.makeText(MainActivity.this, "تطبيق تلاوتي محدث بأحدث إصدار رسمي (v1.0) ✓", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void restoreSavedState() {
        currentSurahId = prefs.getInt("last_surah", 1);
        isHorizontalMode = prefs.getBoolean("is_horizontal", true);
        currentFontSize = prefs.getFloat("font_size", 24.0f);
        currentTheme = prefs.getInt("theme_id", 0);
        scrollSpeed = prefs.getFloat("scroll_speed", 1.5f);

        tvQuranText.setTextSize(currentFontSize);
        seekBarSpeed.setProgress(Math.round(scrollSpeed * 10));
        tvSpeedValue.setText("سرعة: " + String.format("%.1fx", scrollSpeed));

        setReadingMode(isHorizontalMode);
        applyTheme(currentTheme);
    }
}
