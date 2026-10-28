package com.tilawati.quran;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private WebView mWebView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إبقاء الشاشة مضاءة أثناء القراءة والتلاوة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mWebView = new WebView(this);
        setContentView(mWebView);

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);

        // واجهة اتصال الجافاسكريبت مع بيئة الأندرويد الأصلية
        mWebView.addJavascriptInterface(new AndroidBridge(this), "AndroidApp");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // الروابط الخارجية تفتح في المتصفح الخارجي
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (url.contains("alquran.cloud") || url.contains("islamic.network")) {
                        return false; // السماح لجلب البيانات والصوت
                    }
                }
                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient());

        // تحميل ملفات المصحف المحلية من داخل التطبيق مباشرة
        mWebView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * جسر التواصل بين واجهة الويب والأندرويد للتحكم بالتحديث وتنزيل ملف APK
     */
    public class AndroidBridge {
        Context mContext;

        AndroidBridge(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        @JavascriptInterface
        public void downloadAndInstallUpdate(final String apkDownloadUrl) {
            Toast.makeText(mContext, "جاري تنزيل التحديث في الخلفية... ⏳", Toast.LENGTH_LONG).show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(apkDownloadUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.connect();

                        File cacheDir = new File(mContext.getExternalCacheDir(), "updates");
                        if (!cacheDir.exists()) cacheDir.mkdirs();

                        File apkFile = new File(cacheDir, "tilawati-update.apk");
                        FileOutputStream output = new FileOutputStream(apkFile);
                        InputStream input = connection.getInputStream();

                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }

                        output.flush();
                        output.close();
                        input.close();

                        // فتح معالج تثبيت الأندرويد
                        installApk(apkFile);

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "تعذر تنزيل التحديث تلقائياً، يمكنك تنزيله من GitHub.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }

        private void installApk(File apkFile) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(
                        mContext,
                        mContext.getPackageName() + ".fileprovider",
                        apkFile
                );
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }

            mContext.startActivity(intent);
        }
    }
}
