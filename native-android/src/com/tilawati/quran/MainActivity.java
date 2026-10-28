package com.tilawati.quran;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    // عناصر واجهة المستخدم
    private RelativeLayout rootLayout;
    private LinearLayout headerLayout, modeLayout, bottomFlipBar, dockAutoScroll, surahHeaderBox;
    private RelativeLayout mushafContainer;
    private TextView tvAppTitle, tvHeaderSubtitle, tvSurahTitle, tvSurahDetails, tvBismillah, tvQuranText, tvPageIndex, tvSpeedValue;
    private Button btnSurahIndex, btnThemes, btnFontSize, btnModeHorizontal, btnModeVertical;
    private Button btnPrevSurah, btnNextSurah, btnPlayPauseScroll, btnSpeedDown, btnSpeedUp;
    private Button btnBookmark, btnDuaa;
    private SeekBar seekBarSpeed;
    private ScrollView scrollViewQuran;

    // حالة التطبيق
    private int currentSurahId = 1;
    private boolean isHorizontalMode = true;
    private float currentFontSize = 24.0f;
    private int currentTheme = 0; // 0: Classic, 1: Emerald, 2: Night, 3: Blue, 4: Pearl

    // محرك الحركة التلقائية (Auto-Scroll)
    private boolean isAutoScrolling = false;
    private float scrollSpeed = 1.5f;
    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    // بيانات السور
    private JSONObject quranDataJson;
    private JSONArray surahsArray;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إبقاء الشاشة مضاءة أثناء القراءة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("tilawati_prefs", Context.MODE_PRIVATE);

        initViews();
        loadQuranData();
        setupListeners();
        restoreSavedState();

        loadSurah(currentSurahId);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        headerLayout = findViewById(R.id.headerLayout);
        modeLayout = findViewById(R.id.modeLayout);
        bottomFlipBar = findViewById(R.id.bottomFlipBar);
        dockAutoScroll = findViewById(R.id.dockAutoScroll);
        surahHeaderBox = findViewById(R.id.surahHeaderBox);
        mushafContainer = findViewById(R.id.mushafContainer);

        tvAppTitle = findViewById(R.id.tvAppTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvSurahTitle = findViewById(R.id.tvSurahTitle);
        tvSurahDetails = findViewById(R.id.tvSurahDetails);
        tvBismillah = findViewById(R.id.tvBismillah);
        tvQuranText = findViewById(R.id.tvQuranText);
        tvPageIndex = findViewById(R.id.tvPageIndex);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);

        btnSurahIndex = findViewById(R.id.btnSurahIndex);
        btnThemes = findViewById(R.id.btnThemes);
        btnFontSize = findViewById(R.id.btnFontSize);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnDuaa = findViewById(R.id.btnDuaa);
        btnModeHorizontal = findViewById(R.id.btnModeHorizontal);
        btnModeVertical = findViewById(R.id.btnModeVertical);

        btnPrevSurah = findViewById(R.id.btnPrevSurah);
        btnNextSurah = findViewById(R.id.btnNextSurah);
        btnPlayPauseScroll = findViewById(R.id.btnPlayPauseScroll);
        btnSpeedDown = findViewById(R.id.btnSpeedDown);
        btnSpeedUp = findViewById(R.id.btnSpeedUp);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        scrollViewQuran = findViewById(R.id.scrollViewQuran);
    }

    private void loadQuranData() {
        try {
            InputStream is = getAssets().open("quran_data.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();

            quranDataJson = new JSONObject(sb.toString());
            surahsArray = quranDataJson.getJSONArray("surahs");
        } catch (Exception e) {
            e.printStackTrace();
            surahsArray = new JSONArray();
        }
    }

    private void setupListeners() {
        // تبديل الأوضاع
        btnModeHorizontal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReadingMode(true);
            }
        });

        btnModeVertical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReadingMode(false);
            }
        });

        // التقليب في الوضع الجانبي
        btnNextSurah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSurahId < 114) {
                    loadSurah(currentSurahId + 1);
                }
            }
        });

        btnPrevSurah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSurahId > 1) {
                    loadSurah(currentSurahId - 1);
                }
            }
        });

        // الحركة التلقائية (Auto-Scroll)
        btnPlayPauseScroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAutoScroll();
            }
        });

        btnSpeedUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = Math.min(50, seekBarSpeed.getProgress() + 3);
                seekBarSpeed.setProgress(p);
            }
        });

        btnSpeedDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = Math.max(5, seekBarSpeed.getProgress() - 3);
                seekBarSpeed.setProgress(p);
            }
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

        // إيقاف التمرير التلقائي مؤقتاً عند لمس الشاشة
        scrollViewQuran.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && isAutoScrolling) {
                    // لمس المستخدم يوقف الحركة تلقائياً لراحته
                }
                return false;
            }
        });

        // دعم السحب باللمس لليمين واليسار (Swipe Gestures)
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!isHorizontalMode) return false;
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100) {
                    if (diffX > 0) {
                        // سحب لليمين: السورة السابقة
                        if (currentSurahId > 1) loadSurah(currentSurahId - 1);
                    } else {
                        // سحب لليسار: السورة التالية
                        if (currentSurahId < 114) loadSurah(currentSurahId + 1);
                    }
                    return true;
                }
                return false;
            }
        });

        mushafContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        // القوائم: الفهرس، الثيم، الخط
        btnSurahIndex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSurahIndexDialog();
            }
        });

        btnThemes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemeChooserDialog();
            }
        });

        btnFontSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFontSizeDialog();
            }
        });

        btnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBookmarkClick();
            }
        });

        btnDuaa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKhatmDuaaDialog();
            }
        });

        // إعداد دالة التمرير التلقائي الدوري
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoScrolling) {
                    int dy = Math.max(1, Math.round(scrollSpeed * 2));
                    scrollViewQuran.smoothScrollBy(0, dy);

                    // فحص الوصول للنهاية
                    View child = scrollViewQuran.getChildAt(0);
                    if (child != null) {
                        int diff = (child.getBottom() - (scrollViewQuran.getHeight() + scrollViewQuran.getScrollY()));
                        if (diff <= 5) {
                            stopAutoScroll();
                            return;
                        }
                    }
                    autoScrollHandler.postDelayed(this, 30);
                }
            }
        };
    }

    private void setReadingMode(boolean horizontal) {
        isHorizontalMode = horizontal;
        prefs.edit().putBoolean("is_horizontal", isHorizontalMode).apply();

        if (horizontal) {
            btnModeHorizontal.setTextColor(Color.WHITE);
            btnModeHorizontal.setBackgroundColor(Color.parseColor("#8B6B23"));

            btnModeVertical.setTextColor(Color.parseColor("#5C5244"));
            btnModeVertical.setBackgroundColor(Color.parseColor("#EAE3D2"));

            bottomFlipBar.setVisibility(View.VISIBLE);
            dockAutoScroll.setVisibility(View.GONE);
            stopAutoScroll();
        } else {
            btnModeHorizontal.setTextColor(Color.parseColor("#5C5244"));
            btnModeHorizontal.setBackgroundColor(Color.parseColor("#EAE3D2"));

            btnModeVertical.setTextColor(Color.WHITE);
            btnModeVertical.setBackgroundColor(Color.parseColor("#8B6B23"));

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
        btnPlayPauseScroll.setBackgroundColor(Color.parseColor("#2E7D32"));
        autoScrollHandler.post(autoScrollRunnable);
    }

    private void stopAutoScroll() {
        isAutoScrolling = false;
        btnPlayPauseScroll.setText("▶");
        btnPlayPauseScroll.setBackgroundColor(Color.parseColor("#8B6B23"));
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    private void loadSurah(int surahId) {
        currentSurahId = surahId;
        prefs.edit().putInt("last_surah", currentSurahId).apply();

        JSONObject meta = null;
        try {
            for (int i = 0; i < surahsArray.length(); i++) {
                JSONObject s = surahsArray.getJSONObject(i);
                if (s.getInt("id") == surahId) {
                    meta = s;
                    break;
                }
            }
        } catch (Exception e) {}

        String surahName = (meta != null) ? meta.optString("name", "السورة " + surahId) : "السورة " + surahId;
        int totalVerses = (meta != null) ? meta.optInt("totalVerses", 7) : 7;
        String type = (meta != null) ? meta.optString("type", "مكية") : "مكية";
        int page = (meta != null) ? meta.optInt("startPage", 1) : 1;

        tvSurahTitle.setText("سُورَةُ " + surahName);
        tvSurahDetails.setText(type + " • آياتها: " + totalVerses + " • صفحة " + page);
        tvHeaderSubtitle.setText("سورة " + surahName);
        tvPageIndex.setText(surahId + " / 114");

        if (surahId == 9 || surahId == 1) {
            tvBismillah.setVisibility(View.GONE);
        } else {
            tvBismillah.setVisibility(View.VISIBLE);
        }

        btnPrevSurah.setEnabled(surahId > 1);
        btnNextSurah.setEnabled(surahId < 114);

        // جلب نص الآيات
        displaySurahVerses(surahId, totalVerses, surahName);
        scrollViewQuran.scrollTo(0, 0);
    }

    private void displaySurahVerses(int surahId, int totalVerses, String surahName) {
        // 1. فحص السور المدمجة أولاً
        try {
            JSONObject builtin = quranDataJson.optJSONObject("builtin");
            if (builtin != null && builtin.has(String.valueOf(surahId))) {
                JSONObject bObj = builtin.getJSONObject(String.valueOf(surahId));
                JSONArray ayahsArr = bObj.getJSONArray("ayahs");
                SpannableStringBuilder builder = new SpannableStringBuilder();
                for (int i = 0; i < ayahsArr.length(); i++) {
                    JSONObject aObj = ayahsArr.getJSONObject(i);
                    String text = aObj.getString("text");
                    builder.append(text).append(" ۝").append(convertToArabicNumber(i + 1)).append(" ");
                }
                tvQuranText.setText(builder);
                return;
            }
        } catch (Exception e) {}

        // 2. فحص التخزين المحلي
        String cached = prefs.getString("surah_cache_" + surahId, null);
        if (cached != null) {
            tvQuranText.setText(cached);
            return;
        }

        // 3. جلب عبر الإنترنت في الخلفية، مع عرض نص تمهيدي
        tvQuranText.setText("جاري تحميل آيات سورة " + surahName + " المباركة بالرسم العثماني...");
        new FetchSurahTask(surahId, totalVerses, surahName).execute();
    }

    private class FetchSurahTask extends AsyncTask<Void, Void, String> {
        int sId, versesCount;
        String sName;

        FetchSurahTask(int sId, int vCount, String name) {
            this.sId = sId;
            this.versesCount = vCount;
            this.sName = name;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("https://api.alquran.cloud/v1/surah/" + sId + "/quran-uthmani");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();

                JSONObject res = new JSONObject(sb.toString());
                JSONArray ayahs = res.getJSONObject("data").getJSONArray("ayahs");

                StringBuilder fullText = new StringBuilder();
                for (int i = 0; i < ayahs.length(); i++) {
                    String aText = ayahs.getJSONObject(i).getString("text");
                    if (i == 0 && sId != 1 && sId != 9) {
                        aText = aText.replace("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "").trim();
                    }
                    fullText.append(aText).append(" ۝").append(convertToArabicNumber(i + 1)).append(" ");
                }
                return fullText.toString();
            } catch (Exception e) {
                // نص استرشادي عند انقطاع الإنترنت
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= versesCount; i++) {
                    sb.append("الآية الكريمة رقم ").append(convertToArabicNumber(i)).append(" من سورة ").append(sName).append(" ۝").append(convertToArabicNumber(i)).append(" ");
                }
                return sb.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (sId == currentSurahId) {
                tvQuranText.setText(result);
                prefs.edit().putString("surah_cache_" + sId, result).apply();
            }
        }
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
    // الثيمات القرآنية الخمس
    // ==========================================================================
    private void applyTheme(int themeId) {
        currentTheme = themeId;
        prefs.edit().putInt("theme_id", themeId).apply();

        int bgColor, cardColor, textColor, accentColor;

        switch (themeId) {
            case 1: // الروضة الشريفة (الزمردي)
                bgColor = Color.parseColor("#0A241A");
                cardColor = Color.parseColor("#0F3325");
                textColor = Color.parseColor("#F5F2E8");
                accentColor = Color.parseColor("#D4AF37");
                break;
            case 2: // التهجد الليلي
                bgColor = Color.parseColor("#121316");
                cardColor = Color.parseColor("#1A1C22");
                textColor = Color.parseColor("#E6E6E6");
                accentColor = Color.parseColor("#C9B185");
                break;
            case 3: // الأزرق الأندلسي
                bgColor = Color.parseColor("#0D1E38");
                cardColor = Color.parseColor("#132B50");
                textColor = Color.parseColor("#F2F6FC");
                accentColor = Color.parseColor("#E2B963");
                break;
            case 4: // المسك واللؤلؤ
                bgColor = Color.parseColor("#FFFFFF");
                cardColor = Color.parseColor("#F8F9FA");
                textColor = Color.parseColor("#1E2430");
                accentColor = Color.parseColor("#2D5B94");
                break;
            default: // المصحف الكلاسيكي الشاموا
                bgColor = Color.parseColor("#FCF9F2");
                cardColor = Color.parseColor("#FFFDF9");
                textColor = Color.parseColor("#2C251E");
                accentColor = Color.parseColor("#8B6B23");
                break;
        }

        rootLayout.setBackgroundColor(bgColor);
        headerLayout.setBackgroundColor(cardColor);
        bottomFlipBar.setBackgroundColor(cardColor);
        modeLayout.setBackgroundColor(bgColor);

        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardColor);
        cardDrawable.setCornerRadius(30f);
        cardDrawable.setStroke(4, accentColor);
        mushafContainer.setBackground(cardDrawable);

        tvAppTitle.setTextColor(textColor);
        tvHeaderSubtitle.setTextColor(accentColor);
        tvSurahTitle.setTextColor(accentColor);
        tvBismillah.setTextColor(accentColor);
        tvSurahDetails.setTextColor(textColor);
        tvQuranText.setTextColor(textColor);
        tvPageIndex.setTextColor(accentColor);
    }

    private void showThemeChooserDialog() {
        final String[] themes = {
            "📜 المصحف الكلاسيكي (ورق شاموا)",
            "🌿 الروضة الشريفة (أخضر زمردي)",
            "🌙 التهجد الليلي (OLED Dark)",
            "🔷 الأزرق الأندلسي الملكي",
            "🕊️ المسك واللؤلؤ (أبيض ناصع)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر الثيم القرآني:");
        builder.setSingleChoiceItems(themes, currentTheme, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyTheme(which);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showFontSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("حجم خط المصحف:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final TextView tvSize = new TextView(this);
        tvSize.setText("الحجم الحالي: " + Math.round(currentFontSize) + "sp");
        tvSize.setTextSize(16);
        tvSize.setPadding(0, 0, 0, 20);
        layout.addView(tvSize);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(34); // from 18 to 52
        seekBar.setProgress(Math.round(currentFontSize - 18));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFontSize = 18 + progress;
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

    private void showSurahIndexDialog() {
        final ArrayList<String> surahNames = new ArrayList<String>();
        try {
            for (int i = 0; i < surahsArray.length(); i++) {
                JSONObject s = surahsArray.getJSONObject(i);
                surahNames.add(s.getInt("id") + ". سورة " + s.getString("name") + " (" + s.getString("type") + " - " + s.getInt("totalVerses") + " آية)");
            }
        } catch (Exception e) {}

        CharSequence[] items = surahNames.toArray(new CharSequence[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("فهرس سور القرآن الكريم (114 سورة):");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadSurah(which + 1);
            }
        });
        builder.show();
    }

    private void handleBookmarkClick() {
        final int savedBookmark = prefs.getInt("user_bookmark_surah", -1);
        final String savedName = prefs.getString("user_bookmark_name", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("الفاصلة والعلامات المرجعية:");

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
                    Toast.makeText(MainActivity.this, "تم الانتقال إلى الفاصلة!", Toast.LENGTH_SHORT).show();
                } else if (which == 2 && savedBookmark != -1) {
                    prefs.edit().remove("user_bookmark_surah").remove("user_bookmark_name").apply();
                    Toast.makeText(MainActivity.this, "تم حذف الفاصلة.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

