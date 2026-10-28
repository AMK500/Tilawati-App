import os
import sys
import subprocess
import shutil
import zipfile

def build():
    print("==================================================")
    print("🔨 بناء تطبيق تلاوتي الأندرويد الأصلي النقي (100% Native)")
    print("==================================================")

    sdk_root = r"C:\Users\Abdulrahman\AppData\Local\Android\Sdk"
    build_tools = os.path.join(sdk_root, "build-tools", "34.0.0")
    android_jar = os.path.join(sdk_root, "platforms", "android-35", "android.jar")

    aapt2 = os.path.join(build_tools, "aapt2.exe")
    d8 = os.path.join(build_tools, "d8.bat")
    zipalign = os.path.join(build_tools, "zipalign.exe")
    apksigner = os.path.join(build_tools, "apksigner.bat")

    base_dir = r"C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati\native-android"
    src_dir = os.path.join(base_dir, "src", "com", "tilawati", "quran")
    res_dir = os.path.join(base_dir, "res")
    assets_dir = os.path.join(base_dir, "assets")
    manifest = os.path.join(base_dir, "AndroidManifest.xml")
    java_file = os.path.join(src_dir, "MainActivity.java")

    build_dir = os.path.join(base_dir, "build_out")
    os.makedirs(build_dir, exist_ok=True)

    compiled_res_zip = os.path.join(build_dir, "compiled_res.zip")
    gen_dir = os.path.join(build_dir, "gen")
    bin_dir = os.path.join(build_dir, "bin")
    dex_dir = os.path.join(build_dir, "dex")

    for d in [gen_dir, bin_dir, dex_dir]:
        os.makedirs(d, exist_ok=True)

    # 1. Compile resources
    print("\n[1/6] تجميع ملفات الواجهات والموارد (aapt2 compile)...")
    if os.path.exists(compiled_res_zip):
        os.remove(compiled_res_zip)
    cmd_compile = [aapt2, "compile", "--dir", res_dir, "-o", compiled_res_zip]
    subprocess.check_call(cmd_compile)

    # 2. Link resources
    print("[2/6] ربط ملفات التطبيق الأصلية (aapt2 link)...")
    unaligned_apk = os.path.join(build_dir, "unaligned.apk")
    if os.path.exists(unaligned_apk):
        os.remove(unaligned_apk)

    cmd_link = [
        aapt2, "link",
        "-I", android_jar,
        "-A", assets_dir,
        "--manifest", manifest,
        "--min-sdk-version", "21",
        "--target-sdk-version", "34",
        "--version-code", "1",
        "--version-name", "1.0",
        "--java", gen_dir,
        "-o", unaligned_apk,
        compiled_res_zip,
        "--auto-add-overlay"
    ]
    subprocess.check_call(cmd_link)

    # 3. Compile Java source
    print("[3/6] ترجمة كود الجافا الأصلي (javac)...")
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
    print("[4/6] إنشاء كود التشغيل (d8 DEX)...")
    class_files = []
    for root, dirs, files in os.walk(bin_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))

    cmd_d8 = [
        d8,
        "--lib", android_jar,
        "--min-api", "21",
        "--output", dex_dir
    ] + class_files
    subprocess.check_call(cmd_d8, shell=True)

    dex_file = os.path.join(dex_dir, "classes.dex")
    print("إدراج classes.dex في حزمة التطبيق...")
    with zipfile.ZipFile(unaligned_apk, 'a') as zf:
        zf.write(dex_file, "classes.dex")

    # 5. ZipAlign
    print("[5/6] محاذاة ملف الـ APK (zipalign 4-byte)...")
    aligned_apk = os.path.join(build_dir, "aligned.apk")
    if os.path.exists(aligned_apk):
        os.remove(aligned_apk)
    cmd_align = [zipalign, "-p", "-f", "4", unaligned_apk, aligned_apk]
    subprocess.check_call(cmd_align)

    # 6. Key generation and signing
    print("[6/6] توقيع ملف الـ APK بالتوقيع الرسمي (v1/v2/v3)...")
    keystore_path = os.path.join(base_dir, "tilawati_native.keystore")
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
            "-dname", "CN=Tilawati Native, OU=Quran, O=Tilawati, C=SA",
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
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "true",
        "--v3-signing-enabled", "true",
        "--out", final_apk,
        aligned_apk
    ]
    subprocess.check_call(cmd_sign, shell=True)

    # Verify APK with apksigner
    cmd_verify = [apksigner, "verify", "--verbose", final_apk]
    print("\nفحص صحة التوقيع والتوافق...")
    subprocess.check_call(cmd_verify, shell=True)

    print("\n🎉 تم بناء تطبيق الأندرويد الأصلي 100% بنجاح تام!")
    print(f"مسار الملف: {final_apk}")
    print(f"حجم الملف: {os.path.getsize(final_apk) / 1024:.1f} كيلوبايت")

    # 7. Copy to E:\تلاوتي
    dest_dir = r"E:\تلاوتي"
    os.makedirs(dest_dir, exist_ok=True)
    dest_apk = os.path.join(dest_dir, "Tilawati.apk")
    dest_json = os.path.join(dest_dir, "version.json")

    shutil.copy2(final_apk, dest_apk)
    shutil.copy2(r"C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati\version.json", dest_json)

    print(f"\n🚀 تم نقل النسخة الأصلية بنجاح إلى:\n -> {dest_apk}\n -> {dest_json}")

if __name__ == "__main__":
    build()

