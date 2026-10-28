@echo off
chcp 65001 > nul
title بناء وتحديث تطبيق تلاوتي للأندرويد
echo ========================================================
echo 🔨 بناء وتجهيز تحديث جديد لتطبيق تلاوتي (GitHub Release)
echo ========================================================
echo.
set /p VNAME="أدخل رقم الإصدار الجديد (مثال 1.1): "
set /p VCODE="أدخل رقم البناء (Build Code مثال 2): "
set /p NOTES="أدخل ملاحظات التحديث (سجل التغييرات): "

python "C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati\build_release.py" %VCODE% %VNAME% "%NOTES%"

echo.
echo ========================================================
echo 🎉 تم إنشاء النسخة الجديدة بنجاح!
echo كل ما عليك الآن:
echo 1. رفع Tilawati.apk و version.json إلى GitHub:
echo    https://github.com/AMK500/Tilawati-App/releases
echo ========================================================
pause

