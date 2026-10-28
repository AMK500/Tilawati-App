import os
import sys
import json
import subprocess
import shutil

def update_and_build(version_code=None, version_name=None, release_notes=None):
    base_dir = r"C:\Users\Abdulrahman\.gemini\antigravity\scratch\tilawati"
    native_dir = os.path.join(base_dir, "native-android")
    manifest_file = os.path.join(native_dir, "AndroidManifest.xml")
    version_json_file = os.path.join(base_dir, "version.json")
    out_dir = r"E:\تلاوتي"

    # Read current version.json
    with open(version_json_file, 'r', encoding='utf-8') as f:
        v_data = json.load(f)

    if version_code is None:
        version_code = v_data.get("build", 1) + 1
    if version_name is None:
        version_name = f"1.{version_code - 1}"
    if release_notes is None:
        release_notes = "• تحديثات دورية وتحسين في أداء القراءة والتمرير التلقائي."

    import datetime
    today = datetime.date.today().isoformat()

    v_data["version"] = str(version_name)
    v_data["build"] = int(version_code)
    v_data["versionCode"] = int(version_code)
    v_data["versionName"] = str(version_name)
    v_data["releaseDate"] = today
    v_data["title"] = f"تحديث تطبيق تلاوتي v{version_name}"
    v_data["releaseNotes"] = release_notes
    v_data["downloadUrl"] = f"https://github.com/AMK500/Tilawati-App/releases/download/v{version_name}/Tilawati.apk"
    v_data["apkUrl"] = v_data["downloadUrl"]
    v_data["githubRepo"] = "https://github.com/AMK500/Tilawati-App"

    with open(version_json_file, 'w', encoding='utf-8') as f:
        json.dump(v_data, f, ensure_ascii=False, indent=2)

    # Update AndroidManifest.xml
    with open(manifest_file, 'r', encoding='utf-8') as f:
        m_content = f.read()

    import re
    m_content = re.sub(r'android:versionCode=\"\d+\"', f'android:versionCode="{version_code}"', m_content)
    m_content = re.sub(r'android:versionName=\"[^\"]+\"', f'android:versionName="{version_name}"', m_content)

    with open(manifest_file, 'w', encoding='utf-8') as f:
        f.write(m_content)

    print(f"تم تحديث بيانات الإصدار إلى: v{version_name} (Build {version_code})")

    # Run build
    build_script = os.path.join(native_dir, "build_native_apk.py")
    subprocess.check_call([sys.executable, build_script])

    print("==================================================")
    print(f"✅ تم الانتهاء بنجاح! الملفات جاهزة في:")
    print(f" -> {os.path.join(out_dir, 'Tilawati.apk')}")
    print(f" -> {os.path.join(out_dir, 'version.json')}")
    print("==================================================")

if __name__ == "__main__":
    v_code = int(sys.argv[1]) if len(sys.argv) > 1 else None
    v_name = sys.argv[2] if len(sys.argv) > 2 else None
    notes = sys.argv[3] if len(sys.argv) > 3 else None
    update_and_build(v_code, v_name, notes)

