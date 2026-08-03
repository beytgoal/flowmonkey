import json
from .store import get_db, save_db

def view_settings(json_mode=False):
    db = get_db()
    s = db.get("settings", {})

    if json_mode:
        return json.dumps(s, indent=2)

    gemini_key = s.get("geminiApiKey", "")
    highfield_key = s.get("highfieldApiKey", "")
    gemini_str = f"••••••••{gemini_key[-4:]}" if gemini_key else "Belum diatur (Menggunakan Fallback Engine)"
    highfield_str = f"••••••••{highfield_key[-4:]}" if highfield_key else "Terhubung"

    out = f"\n⚙️ PENGATURAN STUDIO & KONFIGURASI API (Python)\n"
    out += f"=========================================================\n"
    out += f"Gemini API Key    : {gemini_str}\n"
    out += f"Highfield Key     : {highfield_str}\n"
    out += f"Custom Endpoint   : {s.get('customEndpoint', '')}\n"
    out += f"Mode Theme        : {'🌙 Dark Theme' if s.get('isDarkTheme', True) else '☀️ Light Theme'}\n"
    out += f"Mode Proxy Active : {'⚡ ON (' + s.get('proxyResolution', '') + ')' if s.get('isProxyModeEnabled') else '🎬 OFF (1080p Original)'}\n"
    out += f"Auto-Transcode    : {'ON' if s.get('autoTranscodeOnImport') else 'OFF'}\n"
    return out

def update_settings(options):
    db = get_db()
    s = db.get("settings", {})

    if "geminiApiKey" in options and options["geminiApiKey"] is not None:
        s["geminiApiKey"] = options["geminiApiKey"]
    if "highfieldApiKey" in options and options["highfieldApiKey"] is not None:
        s["highfieldApiKey"] = options["highfieldApiKey"]
    if "customEndpoint" in options and options["customEndpoint"] is not None:
        s["customEndpoint"] = options["customEndpoint"]
    if "isDarkTheme" in options and options["isDarkTheme"] is not None:
        s["isDarkTheme"] = bool(options["isDarkTheme"])

    db["settings"] = s
    save_db(db)

    if options.get("jsonMode"):
        return json.dumps({"success": True, "settings": s}, indent=2)
    return f"\n✅ Pengaturan Studio berhasil diperbarui!\n"
