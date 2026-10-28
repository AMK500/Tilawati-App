import os
import sys
import subprocess
import shutil
import zipfile

def build():
    print("=== بدء بناء تطبيق تلاوتي للأندرويد (Tilawati APK) ===")
    
    sdk_root = r"C:\Users\Abdulrahman\AppData\Local\Android\Sdk"
    build_tools = os.path.join(sdk_root, "build-tools", "34.0.0")
    android_jar = os.path.join(sdk_root, "platforms", "android-35", "android.jar")
    
    aapt2 = os.path.join(build_tools, "aapt2.exe")
    d8 = os.path.join(build_tools, "d8.bat")
    zipalign = os.path.join(build_tools, "zipalign.exe")
    apksigner = os.path.join(build_tools, "apksigner.bat")
    
    base_dir = r"C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati\android"
    app_dir = os.path.join(base_dir, "app")
    src_main = os.path.join(app_dir, "src", "main")
    
    res_dir = os.path.join(src_main, "res")
    assets_dir = os.path.join(src_main, "assets")
    manifest = os.path.join(src_main, "AndroidManifest.xml")
    java_file = os.path.join(src_main, "java", "com", "tilawati", "quran", "MainActivity.java")
    
    build_dir = os.path.join(base_dir, "build_out")
    os.makedirs(build_dir, exist_ok=True)
    
    compiled_res_zip = os.path.join(build_dir, "compiled_res.zip")
    gen_dir = os.path.join(build_dir, "gen")
    bin_dir = os.path.join(build_dir, "bin")
    os.makedirs(gen_dir, exist_ok=True)
    os.makedirs(bin_dir, exist_ok=True)
    
    # 1. Compile resources with aapt2
    print("[1/6] تجميع الموارد والصور (aapt2 compile)...")
    cmd_compile = [aapt2, "compile", "--dir", res_dir, "-o", compiled_res_zip]
    subprocess.check_call(cmd_compile)
    
    # 2. Link resources with aapt2
    print("[2/6] ربط ملفات التطبيق والمصحف (aapt2 link)...")
    unaligned_apk = os.path.join(build_dir, "unaligned.apk")
    if os.path.exists(unaligned_apk):
        os.remove(unaligned_apk)
        
    cmd_link = [
        aapt2, "link",
        "-I", android_jar,
        "-A", assets_dir,
        "--manifest", manifest,
        "--java", gen_dir,
        "-o", unaligned_apk,
        compiled_res_zip,
        "--auto-add-overlay"
    ]
    subprocess.check_call(cmd_link)
    
    # 3. Compile Java with javac
    print("[3/6] ترجمة أكواد الجافا (javac)...")
    r_java = os.path.join(gen_dir, "com", "tilawati", "quran", "R.java")
    javac_path = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.0-hotspot\bin\javac.exe"
    if not os.path.exists(javac_path):
        javac_path = "javac"
        
    cmd_javac = [
        javac_path,
        "-encoding", "UTF-8",
        "-cp", android_jar,
        "-d", bin_dir,
        java_file,
        r_java
    ]
    subprocess.check_call(cmd_javac)
    
    # 4. Convert classes to dex with d8
    print("[4/6] إنشاء ملف التنفيذ (d8 DEX)...")
    class_files = []
    for root, dirs, files in os.walk(bin_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))
                
    dex_dir = os.path.join(build_dir, "dex")
    os.makedirs(dex_dir, exist_ok=True)
    cmd_d8 = [d8, "--output", dex_dir] + class_files
    subprocess.check_call(cmd_d8, shell=True)
    
    # Add classes.dex into unaligned.apk
    dex_file = os.path.join(dex_dir, "classes.dex")
    print("إضافة classes.dex داخل حزمة الـ APK...")
    with zipfile.ZipFile(unaligned_apk, 'a') as zf:
        zf.write(dex_file, "classes.dex")
        
    # 5. ZipAlign
    print("[5/6] محاذاة الحزمة وضبط الأداء (zipalign)...")
    aligned_apk = os.path.join(build_dir, "aligned.apk")
    if os.path.exists(aligned_apk):
        os.remove(aligned_apk)
    cmd_align = [zipalign, "-p", "-f", "4", unaligned_apk, aligned_apk]
    subprocess.check_call(cmd_align)
    
    # 6. Create keystore if not exists and sign
    print("[6/6] توقيع ملف الـ APK (apksigner)...")
    keystore_path = os.path.join(base_dir, "tilawati-debug.keystore")
    if not os.path.exists(keystore_path):
        keytool_path = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.0-hotspot\bin\keytool.exe"
        if not os.path.exists(keytool_path):
            keytool_path = "keytool"
        cmd_key = [
            keytool_path, "-genkeypair", "-v",
            "-keystore", keystore_path,
            "-storepass", "tilawati123",
            "-alias", "tilawati",
            "-keypass", "tilawati123",
            "-dname", "CN=Tilawati, OU=Quran, O=TilawatiApp, C=SA",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000"
        ]
        subprocess.check_call(cmd_key)
        
    final_apk = os.path.join(build_dir, "Tilawati.apk")
    if os.path.exists(final_apk):
        os.remove(final_apk)
        
    cmd_sign = [
        apksigner, "sign",
        "--ks", keystore_path,
        "--ks-pass", "pass:tilawati123",
        "--ks-key-alias", "tilawati",
        "--key-pass", "pass:tilawati123",
        "--out", final_apk,
        aligned_apk
    ]
    subprocess.check_call(cmd_sign, shell=True)
    
    print("\n✅ تم بناء ملف الـ APK وتوقيعه بنجاح تام!")
    print(f"مسار الـ APK النهائي: {final_apk}")
    print(f"حجم الملف: {os.path.getsize(final_apk) / (1024*1024):.2f} ميجابايت")
    
    # 7. Copy to E:\تلاوتي
    dest_dir = r"E:\تلاوتي"
    os.makedirs(dest_dir, exist_ok=True)
    dest_apk = os.path.join(dest_dir, "Tilawati.apk")
    dest_json = os.path.join(dest_dir, "version.json")
    
    shutil.copy2(final_apk, dest_apk)
    shutil.copy2(r"C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati\version.json", dest_json)
    
    print(f"\n🚀 تم نقل ملف الـ APK وملف الـ JSON بنجاح إلى:\n -> {dest_apk}\n -> {dest_json}")

if __name__ == "__main__":
    build()

